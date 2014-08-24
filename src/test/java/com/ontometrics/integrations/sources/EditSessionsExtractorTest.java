package com.ontometrics.integrations.sources;

import com.ontometrics.integrations.configuration.EventProcessorConfiguration;
import com.ontometrics.integrations.configuration.IssueTracker;
import com.ontometrics.integrations.events.*;
import ontometrics.test.util.TestUtil;
import ontometrics.test.util.UrlStreamProvider;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;
import static org.slf4j.LoggerFactory.getLogger;

public class EditSessionsExtractorTest {

    private Logger log = getLogger(EditSessionsExtractorTest.class);
    private MockIssueTracker mockYouTrackInstance;
    private EditSessionsExtractor editsExtractor;

    @Before
    public void setUp() throws Exception {
        Calendar deploymentTime = Calendar.getInstance();
        deploymentTime.set(Calendar.YEAR, 2013);
        EventProcessorConfiguration.instance().setDeploymentTime(deploymentTime.getTime());
        mockYouTrackInstance = new MockIssueTracker("/feeds/issues-feed-rss.xml", "/feeds/issue-changes.xml");
        editsExtractor = new EditSessionsExtractor(mockYouTrackInstance, UrlStreamProvider.instance());
        editsExtractor.setLastEvent(createProcessEvent());
    }

    @Test
    public void testGetLatestEvents() throws Exception {
        List<IssueEditSession> sessions = editsExtractor.getLatestEdits();

        log.info("latest edits: {}", sessions);
        assertThat(sessions.size(), is(not(0)));
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

    private ProcessEvent createProcessEvent() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyy HH:mm:ss", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return new ProcessEvent.Builder().link("http://ontometrics.com:8085/issue/ASOC-148")
                    .published(dateFormat.parse("Mon, 14 Jul 2014 16:09:07")).title("ASOC-148: New Embedding requirement")
                    .build();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }


    private static class MockIssueTracker implements IssueTracker {
        private String feedUrl;
        private String changesUrl;

        private MockIssueTracker(String feedUrl, String changesUrl) {
            this.feedUrl = feedUrl;
            this.changesUrl = changesUrl;
        }

        @Override
        public URL getBaseUrl() {
            return null;
        }

        @Override
        public URL getFeedUrl() {
            return TestUtil.getFileAsURL(feedUrl);
        }

        @Override
        public URL getChangesUrl(Issue issue) {
            return TestUtil.getFileAsURL(changesUrl);
        }
    }

}