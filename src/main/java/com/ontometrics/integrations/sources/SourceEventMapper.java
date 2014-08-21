
package com.ontometrics.integrations.sources;

import com.ontometrics.integrations.configuration.EventProcessorConfiguration;
import com.ontometrics.integrations.configuration.IssueTracker;
import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.ProcessEvent;
import com.ontometrics.integrations.events.ProcessEventChange;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Does the work of getting Events from the feed, then looks each one up to get
 * the specific changes that were made to them.
 *
 * Created by Rob on 7/11/14.
 * Copyright (c) ontometrics 2014 All rights reserved
 */
public class SourceEventMapper {

    private final IssueTracker issueTracker;
    private Logger log = LoggerFactory.getLogger(SourceEventMapper.class);
    private XMLEventReader eventReader;
    private ProcessEvent lastEvent;
    private StreamProvider streamProvider;

    public SourceEventMapper(IssueTracker issueTracker, StreamProvider streamProvider) {
        this.issueTracker = issueTracker;
        this.streamProvider = streamProvider;
    }

    /**
     * Once we have this open, we should make sure that we are not resending events we have already seen.
     *
     * @return the last event that was returned to the user of this class
     */
    public List<ProcessEvent> getLatestEvents() throws Exception {
        return streamProvider.openResourceStream(issueTracker.getFeedUrl(), is -> {
            LinkedList<ProcessEvent> events = new LinkedList<>();
            try {
                Date deploymentDate = EventProcessorConfiguration.instance().getDeploymentTime();
                XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                eventReader = inputFactory.createXMLEventReader(is);
                DateFormat dateFormat = createEventDateFormat();
                while (eventReader.hasNext()) {
                    XMLEvent nextEvent = eventReader.nextEvent();
                    switch (nextEvent.getEventType()) {
                        case XMLStreamConstants.START_ELEMENT:
                            StartElement startElement = nextEvent.asStartElement();
                            String elementName = startElement.getName().getLocalPart();
                            if (elementName.equals("item")) {
                                //todo: decide if we have to swallow exception thrown by attempt of single event extraction.
                                //If we swallow it, we have at least report the problem
                                ProcessEvent event = extractEventFromStream(dateFormat);

                                if (lastEvent != null && lastEvent.getKey().equals(event.getKey())) {
                                    //we already processed this event before, stopping iteration
                                    return events;
                                }
                                if (event.getPublishDate().after(deploymentDate)) {
                                    //we are adding only events with date after deployment date
                                    events.addFirst(event);
                                }                            
                            }
                        }
                }
            } catch (XMLStreamException e) {
                throw new IOException("Failed to process XML", e);
            }
            return events;
        });
    }

    /**
     * Since the primary interest is in what has been changed, we focus on getting changes
     * often and pushing them into the appropriate channels.
     *
     * @return changes made since we last checked
     */
    public List<ProcessEventChange> getLatestChanges() throws Exception {
        return getLatestEvents().stream()
                .flatMap(e -> {
                    try {
                        return getChanges(e).stream();
                    } catch (Exception ex) {
                        throw new RuntimeException("failed to retrieve changes for event " + e, ex);
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Fetches the changes made to the specified {@link com.ontometrics.integrations.events.Issue}
     *
     * @param e the event from the stream that is actually just the Issue that was touched and when
     * @return the changes that were made, at least one, telling what was changed
     */
    public List<ProcessEventChange> getChanges(ProcessEvent e) throws Exception {
        return getChanges(e, EventProcessorConfiguration.instance().getDeploymentTime());
    }

    public List<ProcessEventChange> getChanges(ProcessEvent e, Date upToDate) throws Exception {
        return streamProvider.openResourceStream(issueTracker.getChangesUrl(e.getIssue()), inputStream -> {
            List<ProcessEventChange> changes = new ArrayList<>();

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
                                XMLEvent insideChangeEvent = reader.nextEvent();
                                log.info("first tag inside change field [type: {}]: {}", insideChangeEvent.getEventType(), insideChangeEvent);
                                StartElement firstValueTag = insideChangeEvent.asStartElement();
                                if (firstValueTag.getName().getLocalPart().equals("oldValue")){
                                    oldValue = reader.getElementText();
                                    reader.nextEvent(); reader.nextEvent();
                                    newValue = reader.getElementText();
                                } else {
                                    newValue = reader.getElementText();
                                }
                            } else {
                                XMLEvent event = reader.nextEvent(); // eat value tag
                                while (event.getEventType() != XMLStreamConstants.START_ELEMENT) {
                                    event = reader.nextEvent();
                                }

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
                            if (upToDate == null || change.getUpdated().after(upToDate)) {
                                changes.add(change);
                            }
                            processingChange = false;
                        }
                        break;

                }
            }
            return changes;
        });
    }

    private DateFormat createEventDateFormat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat;
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

    private ProcessEvent extractEventFromStream(DateFormat dateFormat) throws Exception {
        String prefix;
        int issueNumber;
        String currentTitle = "", currentLink = "", currentDescription = "";
        Date currentPublishDate = null;
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
        String t = currentTitle;
        prefix = t.substring(0, t.indexOf("-"));
        issueNumber = Integer.parseInt(t.substring(t.indexOf("-")+1, t.indexOf(":")));
        ProcessEvent event = new ProcessEvent.Builder()
                .issue(new Issue.Builder().id(issueNumber).projectPrefix(prefix).build())
                .title(currentTitle)
                .description(currentDescription)
                .link(currentLink)
                .published(currentPublishDate)
                .build();
        log.debug("process event extracted and built: {}", event);
        return event;
    }

    private String getEventDate(String date) {
        String UTC = "UT";
        if (date.contains("UT")) {
            return date.substring(0, date.indexOf(UTC));
        }
        return date;
    }

}
