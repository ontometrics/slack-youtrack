package com.ontometrics.integrations.sources;

import com.ontometrics.integrations.configuration.EventProcessorConfiguration;
import com.ontometrics.integrations.configuration.IssueTracker;
import com.ontometrics.integrations.events.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p>
 * Provides a means of seeing what things were changed on an {@link com.ontometrics.integrations.events.Issue} and by whom.
 * Does the work of going first through the feed (RSS) and finding out what tickets
 * have been touched, then looking up changes done to each ticket using the REST interface.
 * </p>
 * <p>
 * Note that the things found in the feed are extracted into the classes {@link com.ontometrics.integrations.events.ProcessEvent}
 * and {@link com.ontometrics.integrations.events.ProcessEventChange}, preserving precisely the information found.
 * But then the changes are converted into {@link com.ontometrics.integrations.events.IssueEdit} instances because they
 * are part of a session that contains the information about who updated them and when.
 * </p>
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
    private Date lastEventDate;
    private StreamProvider streamProvider;

    /**
     * Need to talk to the IssueTracker that has the ticket information, and we will probably
     * have to authenticate, hence the streamProvider.
     *
     * @param issueTracker   the system that is used to track issues
     * @param streamProvider authenticated access to the feed stream
     */
    public EditSessionsExtractor(IssueTracker issueTracker, StreamProvider streamProvider) {
        this.issueTracker = issueTracker;
        this.streamProvider = streamProvider;
    }

    /**
     * Provides a means of seeing what things were changed on an {@link com.ontometrics.integrations.events.Issue} and by whom.
     * Gets a list of IssueEditSessions, being sure to only include edits that were made since we last
     * extracted changes.
     *
     * @return all sessions found that occurred after the last edit
     * @throws Exception
     */
    public List<IssueEditSession> getLatestEdits() throws Exception {
        List<IssueEditSession> sessions = new ArrayList<>();
        List<ProcessEvent> events = getLatestEvents();
        for (ProcessEvent event : events){
            sessions.addAll(getEdits(event, lastEventDate));
        }
        return sessions;
    }

    public List<IssueEditSession> getEdits(final ProcessEvent e, final Date upToDate) throws Exception {
        return streamProvider.openResourceStream(issueTracker.getChangesUrl(e.getIssue()), new InputStreamHandler<List<IssueEditSession>>() {
            @Override
            public List<IssueEditSession> handleStream(InputStream is) throws Exception {
                XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                XMLEventReader eventReader = inputFactory.createXMLEventReader(is);
                //String currentChangeType;
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
                                    //currentChangeType = nextEvent.asStartElement().getAttributes().next().toString();
                                    //log.info("found field named: {}: change type: {}", currentFieldName, currentChangeType);
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
                                    //include only non-processed changes
                                    if (upToDate == null || updated.after(upToDate)) {
                                        if (currentFieldName.equals("resolved")){
                                            newValue = new Date(Long.parseLong(newValue)).toString();
                                        }
                                        ProcessEventChange processEventChange = new ProcessEventChange.Builder()
                                                .updater(updaterName)
                                                .updated(updated)
                                                .field(StringUtils.trim(currentFieldName))
                                                .priorValue(StringUtils.trim(oldValue))
                                                .currentValue(StringUtils.trim(newValue))
                                                .build();

                                        currentChanges.add(processEventChange);
                                        log.info("process event change: {}", processEventChange);
                                    } else {
                                        log.info("process event change skipped due to event-date {} upToDate={}", updated, upToDate);
                                    }
                                    currentFieldName = "";
                                    oldValue = "";
                                    newValue = "";

                                }
                            } else if (tagName.equals("change")) {
                                if (upToDate == null || updated.after(upToDate)) {
                                    List<IssueEdit> edits = buildIssueEdits(currentChanges);
                                    IssueEditSession session = new IssueEditSession.Builder()
                                            .updater(updaterName)
                                            .updated(updated)
                                            .issue(e.getIssue())
                                            .changes(edits)
                                            .build();
                                    extractedEdits.add(session);
                                }
                                currentChanges.clear();
                            }
                            break;

                    }
                }
                return extractedEdits;
            }
        });
    }

    private List<IssueEdit> buildIssueEdits(List<ProcessEventChange> changes) {
        List<IssueEdit> edits = new ArrayList<>(changes.size());
        for (ProcessEventChange change : changes){
            edits.add(new IssueEdit.Builder()
                            .issue(change.getIssue())
                            .field(change.getField())
                            .priorValue(change.getPriorValue())
                            .currentValue(change.getCurrentValue())
                            .build());
        }
        return edits;
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
                                    log.info("last event: {} publish date: {}", lastEventDate, event.getPublishDate());
                                    if (lastEventDate ==null || event.getPublishDate().after(lastEventDate)) {
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
        Issue issue = new Issue.Builder().id(issueNumber).projectPrefix(StringUtils.trim(prefix))
                .title(StringUtils.trim(currentTitle))
                .description(StringUtils.trim(currentDescription))
                .link(new URL(StringUtils.trim(currentLink)))
                .build();
        ProcessEvent event = new ProcessEvent.Builder()
                .issue(issue)
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

    public void setLastEventDate(Date lastEventDate) {
        this.lastEventDate = lastEventDate;
    }

    public Date getLastEventDate() {
        return lastEventDate;
    }
}
