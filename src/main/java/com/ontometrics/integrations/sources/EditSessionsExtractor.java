package com.ontometrics.integrations.sources;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.ontometrics.integrations.configuration.ConfigurationFactory;
import com.ontometrics.integrations.configuration.EventProcessorConfiguration;
import com.ontometrics.integrations.configuration.IssueTracker;
import com.ontometrics.integrations.events.*;
import com.ontometrics.integrations.model.IssueList;
import com.ontometrics.util.BadResponseException;
import com.ontometrics.util.Mapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayInputStream;
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

    private static final Logger responseContentLogger = getLogger("com.ontometrics.integration.youtrack.response");

    private final IssueTracker issueTracker;

    private StreamProvider streamProvider;

    private List<String> excludedFields;

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
        String[] excludedFields = ConfigurationFactory.get().getStringArray("excluded-youtrack-fields");
        this.excludedFields = excludedFields == null ? Collections.<String>emptyList() :
                ImmutableList.copyOf(excludedFields);
    }



    /**
     * Provides a means of seeing what things were changed on an {@link com.ontometrics.integrations.events.Issue} and by whom.
     * Gets a list of IssueEditSessions, being sure to only include edits that were made since we last
     * extracted changes.
     * @param project project key
     * @return all sessions found that occurred after the last edit
     * @throws Exception
     */
    public List<IssueEditSession> getLatestEdits(String project) throws Exception {
        //get events
        EventProcessorConfiguration eventProcessorConfiguration = EventProcessorConfiguration.instance();
        Date minDate = eventProcessorConfiguration
                .resolveMinimumAllowedDate(eventProcessorConfiguration.loadLastProcessedDate(project));

        log.debug("edits since: {} for project {}", minDate, project);

        List<IssueEditSession> sessions = new ArrayList<>();
        List<ProcessEvent> events = getLatestEvents(project, minDate);
        Set<Integer> issuesWeHaveGottenChangesFor = new HashSet<>();
        for (ProcessEvent event : events){
            if (!issuesWeHaveGottenChangesFor.contains(event.getIssue().getId())) {
                issuesWeHaveGottenChangesFor.add(event.getIssue().getId());
                try {
                    List<IssueEditSession> newEdits = getIssueUpdates(event, minDate);
                    sessions.addAll(newEdits);
                } catch (BadResponseException ex) {
                    if (ex.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                        //issue we try to get edits for were deleted so just ignore it
                        log.info("Got 404 response for the issue {}-{}", event.getKey(), event.getIssue());
                    } else {
                        //other cases are not addressed and will be thrown outside
                        throw ex;
                    }
                }
            }
        }
        return sessions;
    }

    private List<IssueEditSession> getIssueUpdates(ProcessEvent event, Date minDate) throws Exception {
        List<IssueEditSession> newEdits = new ArrayList<>();

        List<IssueEditSession> editSessions = getEdits(event, minDate);
        for (IssueEditSession session : editSessions) {
            List<AttachmentEvent> attachmentEvents = getAttachmentEvents(event, minDate);
            boolean hasAttachmentEvents = false;
            if (!attachmentEvents.isEmpty()) {
                hasAttachmentEvents = true;
                newEdits.add(new IssueEditSession.Builder()
                        .updater(attachmentEvents.get(0).getAuthor())
                        .updated(attachmentEvents.get(0).getCreated())
                        .issue(event.getIssue())
                        .attachments(attachmentEvents)
                        .build());
            }
            if (session.hasChanges()) {
                newEdits.add(hasAttachmentEvents ? session.removeAttachments() : session);
            } else {
                if (session.isCreationEdit()) {
                    newEdits.add(hasAttachmentEvents ? session.removeAttachments() : session);
                }
            }

        }

        return newEdits;
    }

    private List<AttachmentEvent> getAttachmentEvents(ProcessEvent event, final Date minDate) throws Exception {
        final URL attachmentsUrl = issueTracker.getAttachmentsUrl(event.getIssue());
        return streamProvider.openResourceStream(attachmentsUrl,
                new InputStreamHandler<List<AttachmentEvent>>() {
            @Override
            public List<AttachmentEvent> handleStream(InputStream is, int responseCode) throws Exception {

                checkResponseCode(responseCode, attachmentsUrl);

                XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                XMLEventReader eventReader = inputFactory.createXMLEventReader(is);
                List<AttachmentEvent> attachmentEvents = new ArrayList<>();
                while (eventReader.hasNext()){
                    XMLEvent nextEvent = eventReader.nextEvent();
                    switch (nextEvent.getEventType()){
                        case XMLStreamConstants.START_ELEMENT:
                            if (nextEvent.asStartElement().getName().getLocalPart().equals("fileUrl")) {
                                String url = nextEvent.asStartElement().getAttributeByName(new QName("", "url")).getValue();
                                String name = nextEvent.asStartElement().getAttributeByName(new QName("", "name")).getValue();
                                String author = nextEvent.asStartElement().getAttributeByName(new QName("", "authorLogin")).getValue();
                                Date created = new Date(Long.parseLong(nextEvent.asStartElement().getAttributeByName(new QName("", "created")).getValue()));
                                if (minDate==null || created.after(minDate)) {
                                    attachmentEvents.add(new AttachmentEvent.Builder().created(created).author(author).url(url).name(name).build());
                                } else {
                                    log.debug("attachment from {} found, before {}", created, minDate);
                                }
                            }
                    }
                }
                log.debug("returning attachment events: {} since: {}", attachmentEvents, minDate);
                return attachmentEvents;
            }
        });
    }

    public List<IssueEditSession> getEdits(final ProcessEvent e, final Date upToDate) throws Exception {
        final URL issueTrackerChangesUrl = issueTracker.getChangesUrl(e.getIssue());
        return streamProvider.openResourceStream(issueTrackerChangesUrl,
                new InputStreamHandler<List<IssueEditSession>>() {
            @Override
            public List<IssueEditSession> handleStream(InputStream is, int responseCode) throws Exception {

                checkResponseCode(responseCode, issueTrackerChangesUrl);

                XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                XMLEventReader eventReader = inputFactory.createXMLEventReader(is);
                //String currentChangeType;
                String currentFieldName = "";
                String oldValue = "", newValue = "";
                String updaterName = "";
                Date updated = null;
                Date created = null;
                String creator = "";
                String description = "";
                List<IssueEditSession> extractedEdits = new ArrayList<>();
                List<ProcessEventChange> currentChanges = new ArrayList<>();
                LinkedHashSet<Comment> newComments = new LinkedHashSet<>();
                List<IssueLink> links = new ArrayList<>();

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
                                case "comment":
                                    Comment newComment = extractCommentFromStream(nextEvent.asStartElement());
                                    if (upToDate == null || newComment.getCreated().after(upToDate)) {
                                        newComments.add(newComment);
                                    }
                                    break;
                                case "created":
                                    currentFieldName = "created";
                                    break;
                                case "updaterFullName":
                                    currentFieldName = "creator";
                                    break;
                                case "creator":
                                    currentFieldName = "creator";
                                    break;
                                case "description":
                                    currentFieldName = "description";
                                    break;
                                case "links":
                                    currentFieldName = "links";
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
                                                switch (currentFieldName) {
                                                    case "updaterName":
                                                        updaterName = elementText;
                                                        break;
                                                    case "updated":
                                                        updated = new Date(Long.parseLong(elementText));
                                                        break;
                                                    case "created":
                                                        created = new Date(Long.parseLong(elementText));
                                                        break;
                                                    case "creator":
                                                        creator = elementText;
                                                        break;
                                                    case "description":
                                                        description = elementText;
                                                        break;
                                                    case "links":
                                                        log.debug("found links");
                                                        IssueLink link = new IssueLink.Builder()
                                                                .type(startElement.getAttributeByName(new QName("", "type")).getValue())
                                                                .role(startElement.getAttributeByName(new QName("", "role")).getValue())
                                                                .relatedIssue(elementText)
                                                                .build();
                                                        links.add(link);
                                                        log.debug("adding link: {}", link);
                                                        break;
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
                            switch (tagName) {
                                case "field":
                                    if (newValue.length() > 0) {
                                        //include only non-processed changes
                                        if (upToDate == null || updated.after(upToDate)) {
                                            if (currentFieldName.equals("resolved")) {
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
                                        }
                                        currentFieldName = "";
                                        oldValue = "";
                                        newValue = "";

                                    }
                                    break;
                                case "change":
                                    if (upToDate == null || updated.after(upToDate)) {
                                        log.debug("upToDate: {} updated: {}", upToDate, updated);
                                        List<IssueEdit> edits = buildIssueEdits(currentChanges);
//                                        for (Comment comment : newComments){
//                                            session = new IssueEditSession.Builder()
//                                                    .updater(comment.getAuthor())
//                                                    .updated(comment.getCreated())
//                                                    .issue(e.getIssue())
//                                                    .comment(comment)
//                                                    .build();
//                                            extractedEdits.add(session);
//                                        }
                                        Issue issue = new Issue.Builder()
                                                .projectPrefix(e.getIssue().getPrefix())
                                                .id(e.getIssue().getId())
                                                .title(e.getIssue().getTitle())
                                                .created(created)
                                                .creator(creator)
                                                .link(e.getIssue().getLink())
                                                .description(description).build();
                                        IssueEditSession session = new IssueEditSession.Builder()
                                                .updater(updaterName)
                                                .updated(updated)
                                                .issue(issue)
                                                .changes(edits)
                                                .build();
                                        extractedEdits.add(session);
                                    } else {
                                        log.debug("skipped change dated: {}", updated);
                                    }
                                    currentChanges.clear();
                                    break;
                            }
                            break;

                    }
                }
                if (upToDate == null || created.after(upToDate)) {
                    Issue newIssue = new Issue.Builder()
                            .projectPrefix(e.getIssue().getPrefix())
                            .id(e.getIssue().getId())
                            .created(created)
                            .creator(updaterName)
                            .description(description)
                            .title(e.getIssue().getTitle())
                            .link(e.getIssue().getLink())
                            .build();
                    IssueEditSession session = new IssueEditSession.Builder()
                            .updater(updaterName)
                            .updated(updated)
                            .issue(newIssue)
                            .links(links)
                            .build();
                    log.info("found new issue created on: {}: {}", created, newIssue);
                    extractedEdits.add(session);
                }
                for (Comment comment : newComments) {
                    if (upToDate == null || comment.getCreated().after(upToDate)) {
                        IssueEditSession session = new IssueEditSession.Builder()
                                .updater(comment.getAuthor())
                                .updated(comment.getCreated())
                                .issue(e.getIssue())
                                .comment(comment)
                                .build();
                        extractedEdits.add(session);
                    }
                }
                return extractedEdits;
            }
        });
    }

    private void checkResponseCode(int responseCode, URL requestUrl) throws BadResponseException {
        if (responseCode != HttpStatus.SC_OK){
            //we got not normal response from server
            throw new BadResponseException(requestUrl, responseCode);
        }
    }

    private Comment extractCommentFromStream(StartElement commentTag) {
        return new Comment.Builder()
                .id(commentTag.getAttributeByName(new QName("", "id")).getValue())
                .author(commentTag.getAttributeByName(new QName("", "authorFullName")).getValue())
                .text(commentTag.getAttributeByName(new QName("", "text")).getValue())
                .deleted(Boolean.valueOf(commentTag.getAttributeByName(new QName("", "deleted")).getValue()))
                .created(new Date(Long.parseLong(commentTag.getAttributeByName(new QName("", "created")).getValue())))
                .build();
    }

    private List<IssueEdit> buildIssueEdits(List<ProcessEventChange> changes) {

        List<IssueEdit> edits = new ArrayList<>(changes.size());
        for (ProcessEventChange change : changes){
            if (!isFieldExcluded(change)) {
                edits.add(new IssueEdit.Builder()
                        .issue(change.getIssue())
                        .field(change.getField())
                        .priorValue(change.getPriorValue())
                        .currentValue(change.getCurrentValue())
                        .build());
            }
        }
        return edits;
    }

    private boolean isFieldExcluded(ProcessEventChange change) {
        return change.getField() == null || excludedFields.contains(change.getField());
    }

    /**
     * Once we have this open, we should make sure that we are not resending events we have already seen.
     *
     * @return the last event that was returned to the user of this class
     */
    public List<ProcessEvent> getLatestEvents(final String project, final Date minDate) throws Exception {
        final URL feedUrl = issueTracker.getFeedUrl(project, minDate);
        log.debug("Going to process url: {}", feedUrl);
        return streamProvider.openResourceStream(feedUrl, new InputStreamHandler<List<ProcessEvent>>() {
            @Override
            public List<ProcessEvent> handleStream(final InputStream is, int responseCode) throws Exception {

                checkResponseCode(responseCode, feedUrl);

                byte[] buf = IOUtils.toByteArray(is);
                ByteArrayInputStream bas = new ByteArrayInputStream(buf);
                if (responseContentLogger.isDebugEnabled()){
                    responseContentLogger.debug("Got response from url: {} \n{}", feedUrl, new String(buf));
                }
                IssueList issueList = Mapper.createXmlMapper().readValue(bas, IssueList.class);
                Iterable<com.ontometrics.integrations.model.Issue> processedIssues = Iterables.filter(issueList.getIssues(), new Predicate<com.ontometrics.integrations.model.Issue>() {
                    @Override
                    public boolean apply(com.ontometrics.integrations.model.Issue issue) {
                        return !issue.getId().toLowerCase().startsWith("draft");
                    }
                });

                return ImmutableList.copyOf(Iterables.transform(processedIssues,
                        new Function<com.ontometrics.integrations.model.Issue, ProcessEvent>() {
                            @Override
                            public ProcessEvent apply(com.ontometrics.integrations.model.Issue issue) {
                                return extractEventFromStream(issue, project);
                            }
                        }));
            }
        });
    }



    private ProcessEvent extractEventFromStream(com.ontometrics.integrations.model.Issue xmlIssue, String project) {

        Issue issue = new Issue.Builder().id(Integer.parseInt(xmlIssue.getFieldValue("numberInProject")))
                .projectPrefix(project)
                .title(StringUtils.trim(xmlIssue.getFieldValue("summary")))
                .description(StringUtils.trim(xmlIssue.getFieldValue("description")))
                .link(issueTracker.getExternalIssueUrl(xmlIssue.getId()))
                .build();


        ProcessEvent event = new ProcessEvent.Builder()
                .issue(issue)
                .published(new Date(Long.parseLong(xmlIssue.getFieldValue("updated"))))
                .build();
        log.debug("event extracted from stream: {}", event);
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


    public IssueTracker getIssueTracker() {
        return issueTracker;
    }

    public StreamProvider getStreamProvider() {
        return streamProvider;
    }
}
