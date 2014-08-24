package com.ontometrics.integrations.sources;

import com.ontometrics.integrations.configuration.EventProcessorConfiguration;
import com.ontometrics.integrations.configuration.IssueTracker;
import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.IssueEditSession;
import com.ontometrics.integrations.events.ProcessEvent;
import com.ontometrics.integrations.events.ProcessEventChange;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * User: Rob
 * Date: 8/23/14
 * Time: 10:19 PM
 * <p/>
 * (c) ontometrics 2014, All Rights Reserved
 */
public class EditSessionsExtractor {

    private Logger log = getLogger(EditSessionsExtractor.class);
    private final IssueTracker issueTracker;
    private XMLEventReader eventReader;
    private ProcessEvent lastEvent;
    private StreamProvider streamProvider;

    /**
     * Provides a means of seeing what things were changed on an {@link com.ontometrics.integrations.events.Issue} and by whom.
     * The changes are grouped into an {@link com.ontometrics.integrations.events.IssueEditSession} because one
     * updater might make multiple changes at one time.
     *
     * @param issueTracker   the system that is used to track issues
     * @param streamProvider authenticated access to the feed stream
     */
    public EditSessionsExtractor(IssueTracker issueTracker, StreamProvider streamProvider) {
        this.issueTracker = issueTracker;
        this.streamProvider = streamProvider;
    }

    public List<IssueEditSession> getLatestEdits() throws Exception {
        List<IssueEditSession> sessions = new ArrayList<>();
        List<ProcessEvent> events = getLatestEvents();
        for (ProcessEvent event : events){
            sessions.addAll(getEdits(event, lastEvent.getPublishDate()));
        }
        return sessions;
    }

    private List<IssueEditSession> getEdits(final ProcessEvent e, final Date upToDate) throws Exception {
        return streamProvider.openResourceStream(issueTracker.getChangesUrl(e.getIssue()), new InputStreamHandler<List<IssueEditSession>>() {
            @Override
            public List<IssueEditSession> handleStream(InputStream is) throws Exception {
                XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                XMLEventReader eventReader = inputFactory.createXMLEventReader(is);
                String currentChangeType;
                String currentFieldName = "";
                String oldValue = "", newValue = "";
                String updaterName = "";
                Date updated = null;
                List<IssueEditSession> extractedEdits = new ArrayList<>();
                List<ProcessEventChange> currentChanges = new ArrayList<>();

                while (eventReader.hasNext()) {
                    XMLEvent nextEvent = eventReader.nextEvent();
                    switch (nextEvent.getEventType()) {
                        case XMLStreamConstants.START_ELEMENT:
                            StartElement startElement = nextEvent.asStartElement();
                            String elementName = startElement.getName().getLocalPart();
                            switch (elementName) {
                                case "change":
                                    break;
                                case "field":
                                    currentFieldName = nextEvent.asStartElement().getAttributeByName(new QName("", "name")).getValue();
                                    currentChangeType = nextEvent.asStartElement().getAttributes().next().toString();
                                    log.info("found field named: {}: change type: {}", currentFieldName, currentChangeType);
                                    break;
                                default:
                                    String elementText;
                                    try {
                                        elementText = eventReader.getElementText();
                                        switch (elementName) {
                                            case "newValue":
                                                newValue = elementText;
                                                break;
                                            case "oldValue":
                                                oldValue = elementText;
                                                break;
                                            case "value":
                                                if (currentFieldName.equals("updaterName")) {
                                                    updaterName = elementText;
                                                } else if (currentFieldName.equals("updated")) {
                                                    updated = new Date(Long.parseLong(elementText));
                                                }
                                        }
                                    } catch (Exception e) {
                                        //no text..
                                    }
                                    break;
                            }
                            break;

                        case XMLStreamConstants.END_ELEMENT:
                            EndElement endElement = nextEvent.asEndElement();
                            String tagName = endElement.getName().getLocalPart();
                            if (tagName.equals("field")) {
                                if (newValue.length() > 0) {
                                    ProcessEventChange processEventChange = new ProcessEventChange.Builder()
                                            .updater(updaterName)
                                            .updated(updated)
                                            .field(currentFieldName)
                                            .priorValue(oldValue)
                                            .currentValue(newValue)
                                            .build();

                                    //include only non-processed changes
                                    if (upToDate == null || processEventChange.getUpdated().after(upToDate)) {
                                        currentChanges.add(processEventChange);
                                    }
                                    currentFieldName = "";
                                    oldValue = "";
                                    newValue = "";

                                    log.info("process event change: {}", processEventChange);
                                }
                            } else if (tagName.equals("change")) {
                                IssueEditSession session = new IssueEditSession.Builder()
                                        .updater(updaterName)
                                        .issue(e.getIssue())
                                        .changes(currentChanges)
                                        .build();
                                extractedEdits.add(session);
                                currentChanges.clear();
                            }
                            break;

                    }
                }
                return extractedEdits;
            }
        });
    }

    /**
     * Once we have this open, we should make sure that we are not resending events we have already seen.
     *
     * @return the last event that was returned to the user of this class
     */
    public List<ProcessEvent> getLatestEvents() throws Exception {
        return streamProvider.openResourceStream(issueTracker.getFeedUrl(), new InputStreamHandler<List<ProcessEvent>>() {
            @Override
            public List<ProcessEvent> handleStream(InputStream is) throws Exception {
                byte[] buf = IOUtils.toByteArray(is);
                ByteArrayInputStream bas = new ByteArrayInputStream(buf);
                LinkedList<ProcessEvent> events = new LinkedList<>();
                try {
                    Date deploymentDate = EventProcessorConfiguration.instance().getDeploymentTime();
                    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                    eventReader = inputFactory.createXMLEventReader(bas);
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
                    throw new IOException("Failed to process XML: content is\n"+new String(buf), e);
                }
                return events;
            }
        });
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

    private DateFormat createEventDateFormat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat;
    }

    private String getEventDate(String date) {
        String UTC = "UT";
        if (date.contains("UT")) {
            return date.substring(0, date.indexOf(UTC));
        }
        return date;
    }

    public void setLastEvent(ProcessEvent lastEvent) {
        this.lastEvent = lastEvent;
    }
}
