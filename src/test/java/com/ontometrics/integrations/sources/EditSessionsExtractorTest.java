package com.ontometrics.integrations.sources;

import com.ontometrics.integrations.configuration.EventProcessorConfiguration;
import com.ontometrics.integrations.configuration.MockIssueTracker;
import com.ontometrics.integrations.configuration.YouTrackInstance;
import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.IssueEdit;
import com.ontometrics.integrations.events.IssueEditSession;
import com.ontometrics.integrations.events.ProcessEvent;
import com.ontometrics.util.DateBuilder;
import ontometrics.test.util.UrlStreamProvider;
import org.hamcrest.Matchers;
import org.hamcrest.number.OrderingComparison;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.Calendar.JULY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.slf4j.LoggerFactory.getLogger;

public class EditSessionsExtractorTest {
    private static final Logger log = getLogger(EditSessionsExtractorTest.class);

    private static final String YOUTRACK_USER = "slackbot";
    private static final String YOUTRACK_PASSWORD = "X9y-86A-bZN-93h";
    private static final String YOUTRACK_URL = "http://ontometrics.com";

    private MockIssueTracker mockYouTrackInstance;
    private EditSessionsExtractor editsExtractor;

    @Before
    public void setUp() throws Exception {
        Calendar deploymentTime = Calendar.getInstance();
        deploymentTime.set(Calendar.YEAR, 2013);
        EventProcessorConfiguration.instance().setDeploymentTime(deploymentTime.getTime());
        mockYouTrackInstance = new MockIssueTracker("/feeds/issues-feed-rss.xml", "/feeds/issue-changes.xml");
        editsExtractor = new EditSessionsExtractor(mockYouTrackInstance, UrlStreamProvider.instance());
        //editsExtractor.setLastEvent(createProcessEvent());
    }

    @Test
    public void testGetLatestEvents() throws Exception {
        List<IssueEditSession> sessions = editsExtractor.getLatestEdits();

        log.info("latest edits: {}", sessions);
        assertThat(sessions.size(), is(not(0)));

        int editCount = 0;
        for (IssueEdit edit : sessions.get(0).getChanges()){
            log.info("change #{}: {}", editCount++, edit);
        }
    }

    @Test
    public void changesToUpdatedShouldNotBeTreatedAsAFieldChange() throws Exception {
        for (IssueEditSession session : editsExtractor.getLatestEdits()){
            for (IssueEdit edit : session.getChanges()){
                if (edit.getField().equals("updated")){
                    fail("updated is a timestamp, not a separate change field.");
                }
            }
        }
    }

    @Test
    public void testThatWeCanReadFromFile() throws IOException, XMLStreamException {
        int startElementCount = 0;
        int endElementCount = 0;
        InputStream inputStream = mockYouTrackInstance.getFeedUrl().openStream();
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLStreamReader reader = inputFactory.createXMLStreamReader(inputStream);

        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    log.info("start element");
                    startElementCount++;
                case XMLStreamReader.ATTRIBUTE:
                    log.info("end element");
                    endElementCount++;
            }
        }
        assertThat(startElementCount, Matchers.is(Matchers.not(0)));
        assertThat(endElementCount, Matchers.is(equalTo(startElementCount)));
    }

    @Test
    public void testThatWeCanReadAsEventStream() throws IOException, XMLStreamException {
        InputStream inputStream = mockYouTrackInstance.getFeedUrl().openStream();
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = inputFactory.createXMLEventReader(inputStream);

        while (eventReader.hasNext()) {
            log.info("event: {}", eventReader.nextEvent());
        }
    }

    @Test
    public void testCanParseDate() {
        Date date = new DateBuilder().day(14).month(JULY).year(2014).hour(16).minutes(41).seconds(3).build();
        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zz");

        log.info("date: {}", df.format(date));
    }

    @Test
    public void testGettingLocalFileAsUrlWorks(){
        assertThat(mockYouTrackInstance.getFeedUrl(), notNullValue());
    }

    @Test
    public void testThatWeCanGetMostRecentEdits() throws Exception {
        EditSessionsExtractor editSessionsExtractor = new EditSessionsExtractor(mockYouTrackInstance,
                UrlStreamProvider.instance());
        List<IssueEditSession> recentEdits = editSessionsExtractor.getLatestEdits();

        log.info("recent changes: {}", recentEdits);
        assertThat(recentEdits.size(), Matchers.is(450));

    }

    @Test
    /**
     * Tests that {@link com.ontometrics.integrations.sources.SourceEventMapper} initialized with specified lastEvent
     * field returns correct list of latest events (does not return already processed events)
     */
    public void testThatLastEventIsCorrectlyUsedToRetrieveLatestEvents() throws Exception {
        EditSessionsExtractor editSessionsExtractor = new EditSessionsExtractor(mockYouTrackInstance,
                UrlStreamProvider.instance());
        //14 Jul 2014 16:09:07
        editSessionsExtractor.setLastEvent(new DateBuilder().day(14).month(Calendar.JULY).hour(16)
                .minutes(9).seconds(7).build());
        List<ProcessEvent> latestEvents = editSessionsExtractor.getLatestEvents();
        assertThat(latestEvents.size(), Matchers.is(16));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testThatEventChangesAreParsed() throws Exception {
        mockYouTrackInstance = new MockIssueTracker("/feeds/issues-feed-rss.xml", "/feeds/issue-changes2.xml");
        EditSessionsExtractor sessionsExtractor = new EditSessionsExtractor(mockYouTrackInstance, UrlStreamProvider.instance());
        List<IssueEditSession> edits = sessionsExtractor.getEdits(createProcessEvent());
        assertThat(edits, Matchers.not(empty()));
    }

    @Test
    public void testThatRSSRawFileCanBeRead() throws Exception {
        mockYouTrackInstance = new MockIssueTracker("/feeds/issues-feed-rss-2.xml", "/feeds/issue-changes.xml");
        EditSessionsExtractor sourceEventMapper = new EditSessionsExtractor(mockYouTrackInstance, UrlStreamProvider.instance());
        List<ProcessEvent> changes = sourceEventMapper.getLatestEvents();
        assertThat(changes, Matchers.not(empty()));

    }

    @Test
    public void testThatWeCanGetEventsFromRealFeed() throws Exception {
        YouTrackInstance youTrackInstance = new YouTrackInstance.Builder().baseUrl(YOUTRACK_URL).port(8085).build();
        StreamProvider streamProvider = AuthenticatedHttpStreamProvider.basicAuthenticatedHttpStreamProvider(
                YOUTRACK_USER, YOUTRACK_PASSWORD
        );
        EditSessionsExtractor sourceEventMapper = new EditSessionsExtractor(youTrackInstance, streamProvider);
        List<ProcessEvent> changes = sourceEventMapper.getLatestEvents();
        assertThat(changes, Matchers.not(empty()));
    }


    @Test
    /**
     * Tests that {@link com.ontometrics.integrations.sources.EditSessionsExtractor#getEdits(com.ontometrics.integrations.events.ProcessEvent, java.util.Date)}
     * return the changes which happened after the "upToDate"
     */
    public void testThatOnlyChangesAfterSpecifiedDateAreIncluded() throws Exception {
        EditSessionsExtractor editSessionsExtractor = new EditSessionsExtractor(
                mockYouTrackInstance, UrlStreamProvider.instance());


        List<IssueEditSession> allEditSessions = editSessionsExtractor.getEdits(createProcessEvent());
        //all changes should be included if no date is specified
        assertThat(allEditSessions, hasSize(9));


        Date minDate = new Date(1407626732316L);
        editSessionsExtractor.setLastEvent(minDate);
        List<IssueEditSession> changesAfterDate = editSessionsExtractor.getEdits(createProcessEvent());
        assertThat(changesAfterDate, hasSize(4));
        for (IssueEditSession issueEditSession : changesAfterDate) {
            assertThat(issueEditSession.getUpdated(), OrderingComparison.greaterThan(minDate));
        }
    }

    private ProcessEvent createProcessEvent() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyy HH:mm:ss", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String title = "ASOC-148: New Embedding requirement";
        try {
            String link = "http://ontometrics.com:8085/issue/ASOC-148";
            return new ProcessEvent.Builder().issue(new Issue.Builder().id(148).link(new URL(link)).title(title).build())
                    .published(dateFormat.parse("Mon, 14 Jul 2014 16:09:07"))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}