package ontometrics.integrations.sources;

import com.ontometrics.integrations.configuration.EventProcessorConfiguration;
import com.ontometrics.integrations.configuration.IssueTracker;
import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.ProcessEvent;
import com.ontometrics.integrations.events.ProcessEventChange;
import com.ontometrics.integrations.sources.SourceEventMapper;
import com.ontometrics.util.DateBuilder;
import ontometrics.test.util.TestUtil;
import ontometrics.test.util.UrlStreamProvider;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class SourceEventMapperTest {

    private static final Logger log = LoggerFactory.getLogger(SourceEventMapperTest.class);

    private IssueTracker mockYouTrackInstance;

    @Before
    public void setup() throws Exception {
        Calendar deploymentTime = Calendar.getInstance();
        deploymentTime.set(Calendar.YEAR, 2013);
        EventProcessorConfiguration.instance().setDeploymentTime(deploymentTime.getTime());
        mockYouTrackInstance = new IssueTracker(){
            @Override
            public URL getBaseUrl() {
                return null;
            }

            @Override
            public URL getFeedUrl() {
                return TestUtil.getFileAsURL("/feeds/issues-feed-rss.xml");
            }

            @Override
            public URL getChangesUrl(Issue issue) {
                return TestUtil.getFileAsURL("/feeds/issue-changes.xml");
            }
        };
    }

    @Test
    public void testGettingLocalFileAsUrlWorks(){
        assertThat(mockYouTrackInstance.getFeedUrl(), notNullValue());
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
        assertThat(startElementCount, is(not(0)));
        assertThat(endElementCount, is(equalTo(startElementCount)));
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
    public void testThatWeCanExtractYouTrackEvent() throws Exception {
        
        SourceEventMapper sourceEventMapper = new SourceEventMapper(mockYouTrackInstance, UrlStreamProvider.instance());
        List<ProcessEvent> events = sourceEventMapper.getLatestEvents();

        assertThat(events.size(), is(50));

        ProcessEvent firstEvent = events.get(events.size()-1);

        // check that we got the issue information parsed properly
        assertThat(firstEvent.getIssue().getId(), is(28));
        assertThat(firstEvent.getIssue().getPrefix(), is("ASOC"));

        assertThat(firstEvent.getTitle(), is("ASOC-28: User searches for Users by name"));
        assertThat(firstEvent.getLink(), is("http://ontometrics.com:8085/issue/ASOC-28"));
        assertThat(firstEvent.getDescription(), is("<table> <tr> <th>Reporter</th> <td> <img src=\"http://ontometrics.com:8085/_classpath/smartui/img/youPicture.gif\" width=\"56\" height=\"59\" alt=\"Tim Fulmer (timfulmer)\" title=\"Tim Fulmer (timfulmer)\"/> Tim Fulmer (timfulmer) </td> </tr> <tr> <th>Created</th> <td>Apr 15, 2014 9:05:53 AM</td> </tr> <tr> <th>Updated</th> <td>Jul 14, 2014 9:41:03 AM</td> </tr> <tr> <th>Priority</th> <td>Normal</td> </tr> <tr> <th>Type</th> <td>Feature</td> </tr> <tr> <th>State</th> <td>Open</td> </tr> <tr> <th>Assignee</th> <td>Noura Hassan (noura)</td> </tr> <tr> <th>Subsystem</th> <td>No subsystem</td> </tr> <tr> <th>Fix versions</th> <td>1.0.0</td> </tr> <tr> <th>Affected versions</th> <td>Unknown</td> </tr> <tr> <th>Fixed in build</th> <td>Next Build</td> </tr> </table> <div class=\"wiki text\">User can search for other Users by name, first screenshot. Users can follow in the search results. Tapping on a search result shows the selected User&#39;s profile page.<br/><br/>API service call:<br/><br/><a href=\"http://devvixletapi-env.elasticbeanstalk.com/#!/search/_get_0\">http://devvixletapi-env.elasticbeanstalk.com/#!/search/_get_0</a></div>"));

        log.info("size of events: {}", events.size());
        assertThat(events.size(), is(not(0)));
    }

    @Test
    @Ignore
    /**
     * This test has been ignored, since SourceEventMapper is not responsible anymore for updating lastEvent inside
     * {@link SourceEventMapper#getLatestEvents}, it is updated outside of SourceEventMapper by external code
     * On the moment it is updated by {@link com.ontometrics.integrations.jobs.EventListenerImpl#postEventChangesToStream} )
     * after <strong>successful</strong> completion of processing of each {@link com.ontometrics.integrations.events.ProcessEvent}
     */
    public void testThatLastSeenEventTracked() throws Exception {
        SourceEventMapper sourceEventMapper = new SourceEventMapper(mockYouTrackInstance, UrlStreamProvider.instance());
        List<ProcessEvent> events = sourceEventMapper.getLatestEvents();
        ProcessEvent lastEventFromFetch = events.get(events.size()-1);

        log.info("last event from fetch: {}", lastEventFromFetch);

        assertThat(sourceEventMapper.getLastEvent(), is(lastEventFromFetch));
    }

    @Test
    public void testThatEventsStreamProcessed() throws Exception {

        SourceEventMapper sourceEventMapper = new SourceEventMapper(mockYouTrackInstance, UrlStreamProvider.instance());
        List<ProcessEvent> events = sourceEventMapper.getLatestEvents();

        ChannelMapper channelMapper = new ChannelMapper.Builder()
                .defaultChannel("process")
                .addMapping("ASOC", "vixlet")
                .addMapping("DMAN", "dminder")
                .build();

        events.stream().forEach(e -> postEventToChannel(e, channelMapper.getChannel(e)));

    }

    @Test
    public void testThatWeCanGetSlackUserList(){
        String token = "xoxp-2427064028-2427064030-2467602952-3d5dc6";

        Client client = ClientBuilder.newClient();
        String slackUrl = "https://slack.com/api/users.list?token=" + token;
        WebTarget slackApi = client.target(slackUrl);

        Invocation.Builder invocationBuilder = slackApi.request(MediaType.APPLICATION_JSON);
        Response response = invocationBuilder.get();

        log.info("response code: {} response: {}", response.getStatus(), response.readEntity(String.class));

    }

    @Test
    public void testThatChannelCanBePostedTo(){
        String token = "xoxp-2427064028-2427064030-2467602952-3d5dc6";

        Client client = ClientBuilder.newClient();
        String slackUrl = "https://slack.com/api/";
        String channelPostPath = "chat.postMessage";

        WebTarget slackApi = client.target(slackUrl).path(channelPostPath)
                .queryParam("token", token)
                .queryParam("text", "hi there from unit test...")
                .queryParam("channel", "#process");

        Invocation.Builder invocationBuilder = slackApi.request(MediaType.APPLICATION_JSON);
        Response response = invocationBuilder.get();

        log.info("response code: {} response: {}", response.getStatus(), response.readEntity(String.class));

    }

    @Test
    public void testThatWeCanGetMostRecentChanges() throws Exception {
        SourceEventMapper sourceEventMapper = new SourceEventMapper(mockYouTrackInstance, UrlStreamProvider.instance());
        List<ProcessEventChange> recentChanges;
        recentChanges = sourceEventMapper.getLatestChanges();

        log.info("recent changes: {}", recentChanges);

        assertThat(recentChanges.size(), is(200));

    }

    @Test
    public void testThatWeGetRelationChanges(){
        


    }

    @Test
    /**
     * Tests that {@link com.ontometrics.integrations.sources.SourceEventMapper} initialized with specified lastEvent
     * field returns correct list of latest events (does not return already processed events)
     */
    public void testThatLastEventIsCorrectlyUsedToRetrieveLatestEvents() throws Exception {
        SourceEventMapper sourceEventMapper = new SourceEventMapper(mockYouTrackInstance, UrlStreamProvider.instance());
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyy HH:mm:ss", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        sourceEventMapper.setLastEvent(new ProcessEvent.Builder().link("http://ontometrics.com:8085/issue/ASOC-148")
                .published(dateFormat.parse("Mon, 14 Jul 2014 16:09:07")).title("ASOC-148: New Embedding requirement")
                .build());
        List<ProcessEvent> latestEvents = sourceEventMapper.getLatestEvents();
        assertThat(latestEvents.size(), is(9));
    }


    private void postEventToChannel(ProcessEvent event, String channel) {
        log.info("posting: {} to channel: {}", event.getTitle(), channel);
    }

}