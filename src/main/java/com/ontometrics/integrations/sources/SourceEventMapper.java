
package com.ontometrics.integrations.sources;

import com.ontometrics.integrations.events.ProcessEvent;
import com.ontometrics.integrations.events.ProcessEventChange;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by rob on 7/11/14.
 * Copyright (c) ontometrics 2014 All rights reserved
 */
public class SourceEventMapper {

    private Logger log = LoggerFactory.getLogger(SourceEventMapper.class);

    private URL editsUrl;
    private XMLEventReader eventReader;

    private InputStreamProvider inputStreamProvider;

    private ProcessEvent lastEvent;


    public SourceEventMapper(InputStreamProvider inputStreamProvider) {
        this.inputStreamProvider = inputStreamProvider;
    }

    /**
     * Once we have this open, we should make sure that we are not resending events we have already seen.
     *
     * @return the last event that was returned to the user of this class
     */
    public List<ProcessEvent> getLatestEvents(){

        List<ProcessEvent> events = new ArrayList<>();
        InputStream inputStream = null;
        try {
            inputStream = inputStreamProvider.openStream();
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            eventReader = inputFactory.createXMLEventReader(inputStream);
            DateFormat dateFormat = createEventDateFormat();
            while (eventReader.hasNext()) {
                XMLEvent nextEvent = eventReader.nextEvent();
                switch (nextEvent.getEventType()){
                    case XMLStreamConstants.START_ELEMENT:
                        StartElement startElement = nextEvent.asStartElement();
                        String elementName = startElement.getName().getLocalPart();
                        if (elementName.equals("item")) {
                            ProcessEvent event = extractEventFromStream(dateFormat);

                            if (lastEvent != null && lastEvent.getKey().equals(event.getKey())) {
                                //we already processed this event before, stopping iteration
                                return events;
                            }
                            events.add(event);
                        }
                }
            }
        } catch (XMLStreamException | IOException e) {
            log.error("Failed to process XML", e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        if (!events.isEmpty()) {
            lastEvent = events.get(events.size() - 1);
        }
        return events;
    }

    /**
     * Since the primary interest is in what has been changed, we focus on getting changes
     * often and pushing them into the appropriate channels.
     *
     * @return changes made since we last checked
     */
    public List<ProcessEventChange> getLatestChanges(){
        return getLatestEvents().stream()
                .flatMap(e -> getChanges(e).stream())
                .collect(Collectors.toList());
    }

    private List<ProcessEventChange> getChanges(ProcessEvent e) {
        List<ProcessEventChange> changes = new ArrayList<>();
        try {
            URL changesUrl = buildEventChangesUrl(e.getID());
            InputStream inputStream = changesUrl.openStream();
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLEventReader reader = inputFactory.createXMLEventReader(inputStream);
            boolean processingChange = false;
            String fieldName = null;
            String oldValue = null, newValue = null;
            String updater = null;
            Date updated = null;
            while (reader.hasNext()){
                XMLEvent nextEvent = reader.nextEvent();
                switch (nextEvent.getEventType()){
                    case XMLStreamConstants.START_ELEMENT:
                        StartElement startElement = nextEvent.asStartElement();
                        String elementName = startElement.getName().getLocalPart();
                        if (elementName.equals("change")){
                            processingChange = true;
                        }
                        if (elementName.equals("field") && processingChange){
                            fieldName = startElement.getAttributeByName(new QName("", "name")).getValue();
                            boolean isChangeField = startElement.getAttributes().next().toString().contains("ChangeField");
                            if (isChangeField){
                                reader.nextEvent();
                                StartElement firstValueTag = reader.nextEvent().asStartElement();
                                if (firstValueTag.getName().getLocalPart().equals("oldValue")){
                                    oldValue = reader.getElementText();
                                    reader.nextEvent(); reader.nextEvent();
                                    newValue = reader.getElementText();
                                } else {
                                    newValue = reader.getElementText();
                                }
                            } else {
                                reader.nextEvent(); // eat value tag
                                reader.nextEvent();
                                String fieldValue = reader.getElementText();
                                if (fieldName.equals("updaterName")){
                                    updater = fieldValue;
                                } else if (fieldName.equals("updated")){
                                    updated = new Date(Long.parseLong(fieldValue));
                                }
                            }
                        }
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        EndElement endElement = nextEvent.asEndElement();
                        if (endElement.getName().getLocalPart().equals("change")){
                            ProcessEventChange change = new ProcessEventChange.Builder()
                                    .updater(updater)
                                    .updated(updated)
                                    .field(fieldName)
                                    .priorValue(oldValue)
                                    .currentValue(newValue)
                                    .build();
                            log.info("change: {}", change);
                            changes.add(change);
                            processingChange = false;
                        }
                        break;

                }
            }
        } catch (IOException | XMLStreamException e1) {
            e1.printStackTrace();
        }
        return changes;
    }

    private DateFormat createEventDateFormat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat;
    }

    private URL buildEventChangesUrl(String issueID) throws MalformedURLException {
        URL changesUrl = editsUrl;
        if (editsUrl.toString().contains("{issue}")){
            changesUrl = new URL(editsUrl.toString().replace("{issue}", issueID));
        }
        return changesUrl;
    }

    /**
     * Given that the stream should automatically do this, this might not be needed.
     *
     * @return the last event returned the last time #getLatestEvents() was called.
     */
    public ProcessEvent getLastEvent() {
        return lastEvent;
    }

    public void setLastEvent(ProcessEvent lastEvent) {
        this.lastEvent = lastEvent;
    }

    private ProcessEvent extractEventFromStream(DateFormat dateFormat) {
        String currentTitle = "", currentLink = "", currentDescription = "";
        Date currentPublishDate = null;
        try {
            eventReader.nextEvent();
            StartElement titleTag = eventReader.nextEvent().asStartElement(); // start title tag
            if ("title".equals(titleTag.getName().getLocalPart())){
                currentTitle = eventReader.getElementText();
                eventReader.nextEvent(); // eat end tag
                eventReader.nextEvent();
                currentLink = eventReader.getElementText();
                eventReader.nextEvent(); eventReader.nextEvent();
                currentDescription = eventReader.getElementText().replace("\n", "").trim();
                eventReader.nextEvent(); eventReader.nextEvent();
                currentPublishDate = dateFormat.parse(getEventDate(eventReader.getElementText()));
            }
        } catch (XMLStreamException | ParseException e) {
            e.printStackTrace();
        }
        ProcessEvent event = new ProcessEvent.Builder()
                .title(currentTitle)
                .description(currentDescription)
                .link(currentLink)
                .published(currentPublishDate)
                .build();
        log.info("{}", event);
        return event;
    }

    private String getEventDate(String date) {
        String UTC = "UT";
        if (date.contains("UT")) {
            return date.substring(0, date.indexOf(UTC));
        }
        return date;
    }

    public void setEditsUrl(URL editsUrl) {
        this.editsUrl = editsUrl;
    }
}
