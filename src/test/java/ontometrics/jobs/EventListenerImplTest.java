package ontometrics.jobs;

import com.ontometrics.integrations.configuration.*;
import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.IssueEditSession;
import com.ontometrics.integrations.events.ProcessEvent;
import com.ontometrics.integrations.jobs.EventListenerImpl;
import com.ontometrics.integrations.sources.ChannelMapper;
import com.ontometrics.integrations.sources.EditSessionsExtractor;
import ontometrics.test.util.TestUtil;
import ontometrics.test.util.UrlStreamProvider;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;


/**
 * EventListenerImplTest.java

 */
public class EventListenerImplTest {

    @Test
    public void testThatSourceEventMapperCorrectlyInitializedOnFirstStart() throws ConfigurationException {
        EventProcessorConfiguration configuration = EventProcessorConfiguration.instance();
        configuration.clearLastProcessEvent();
        EventListenerImpl eventListener = createEventListener();
        assertThat(eventListener.getEditSessionsExtractor(), notNullValue());
        assertThat(eventListener.getEditSessionsExtractor().getLastEvent(), nullValue());
    }

    @Test
    public void testThatSourceEventMapperCorrectlyInitializedWithExistingConfiguration() throws ConfigurationException, MalformedURLException {

        EventProcessorConfiguration configuration = EventProcessorConfiguration.instance();
        Issue issue = new Issue.Builder().projectPrefix("ASOC").id(28)
                .title("ASOC-28: title")
                .link(new URL("http://ontometrics.com:8085/issue/ASOC-28"))
                .build();
        ProcessEvent processEvent = new ProcessEvent.Builder().issue(issue).published(new Date()).build();
        configuration.saveLastProcessedEventDate(processEvent.getPublishDate());

        EventListenerImpl eventListener = createEventListener();
        assertThat(eventListener.getEditSessionsExtractor(), notNullValue());
        assertThat(eventListener.getEditSessionsExtractor().getLastEvent(), notNullValue());
//        assertThat(eventListener.getEditSessionsExtractor().getLastEvent().getIssue().getLink(), is(processEvent.getIssue().getLink()));
//        assertThat(eventListener.getEditSessionsExtractor().getLastEvent().getPublishDate(), is(processEvent.getPublishDate()));

    }

    @Test
    /**
     * Tests that all new issue edits are fetched by {@link com.ontometrics.integrations.jobs.EventListenerImpl#checkForNewEvents()}
     *
     * Current last issue date is <code>t0</code>.
     * Issue 1 has changes with time line: (T-1), T1, T3
     * Issue 2 has changes with time line: (T-2), T2, T5
     * Relation between those time stamps are as follows
     *                        T0
     * Issue 1: (T-1)              T1        T3
     * Issue 2:       (T-2)             T2        T5
     *
     * Expected edits for issue 1: (t1) and (t3)
     * Expected edits for issue 2: (t2) and (t5)
     */
    public void testThatAllNewEditsAreFetched() throws Exception {
        final Date T_MINUS_2 = new Date(1404927519000L);
        final Date T1 = new Date(1404927529000L);
        final Date T0 = new Date((T_MINUS_2.getTime() + T1.getTime()) / 2);
        final Date T2 = new Date(1404927539000L);
        final Date T3 = new Date(1404927549000L);
        final Date T5 = new Date(1404927559000L);

        final AtomicInteger issueOrder = new AtomicInteger(0);
        EventProcessorConfiguration.instance().clearLastProcessEvent();
        MockIssueTracker mockYouTrackInstance = new MockIssueTracker("/feeds/issues-feed-rss.xml", null) {
            @Override
            public URL getChangesUrl(Issue issue) {
                if (issueOrder.get() == 0) {
                    //for issue 1 return first set of changes
                    return TestUtil.getFileAsURL("/feeds/issue1-timeline-changes.xml");
                }
                //for issue 2 return second set of changes
                return TestUtil.getFileAsURL("/feeds/issue2-timeline-changes.xml");
            }
        };
        EditSessionsExtractor sessionsExtractor = new EditSessionsExtractor(mockYouTrackInstance,
                UrlStreamProvider.instance()) {
            @Override
            public List<IssueEditSession> getEdits(ProcessEvent e) throws Exception {
                List<IssueEditSession> sessions = super.getEdits(e);
                if (issueOrder.get() == 0) {
                    //asserting that all edits for issue 1 are fetched (T1 and T3)
                    assertThat(sessions.size(), is(2));
                    assertThat(sessions.get(0).getUpdated(), is(T1));
                    assertThat(sessions.get(1).getUpdated(), is(T3));
                } else {
                    //asserting that all edits for issue 2 are fetched (T2 and T5)
                    assertThat(sessions.size(), is(2));
                    assertThat(sessions.get(0).getUpdated(), is(T2));
                    assertThat(sessions.get(1).getUpdated(), is(T5));
                }
                //issue is processed, edits for it are loaded, increasing issue #
                issueOrder.incrementAndGet();
                return sessions;
            }

            @Override
            /**
             * Emulates getting of only two latest events with publish date more than {@link T0}
             */
            public List<ProcessEvent> getLatestEvents() throws Exception {
                List<ProcessEvent> events = super.getLatestEvents().subList(0, 2);
                events.get(0).setPublishDate(T3);
                events.get(1).setPublishDate(T5);
                this.setLastEvent(T0);
                return events;
            }
        };
        EventListenerImpl eventListener = new EventListenerImpl(sessionsExtractor, new EmptyChatServer());
        int events = eventListener.checkForNewEvents();
        //asserting that there are 2 issues processed
        assertThat(issueOrder.get(), is(2));
        assertThat(events, is(2));
    }


    private EventListenerImpl createEventListener() {
        return new EventListenerImpl(UrlStreamProvider.instance(), new SlackInstance.Builder()
                .channelMapper(createChannelMapper()).build());
    }


    /**
     * @return channel mapper with default projects: ASOC, Job Spider and Dminder
     */
    private static ChannelMapper createChannelMapper() {
        return new ChannelMapper.Builder()
                .defaultChannel("process")
                .addMapping("ASOC", "vixlet")
                .addMapping("HA", "jobspider")
                .addMapping("DMAN", "dminder")
                .build();
    }

}
