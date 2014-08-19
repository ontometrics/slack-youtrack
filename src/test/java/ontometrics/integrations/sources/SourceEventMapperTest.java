package ontometrics.integrations.sources;

import com.ontometrics.integrations.events.ProcessEvent;
import com.ontometrics.integrations.events.ProcessEventChange;
import com.ontometrics.integrations.sources.SourceEventMapper;
import com.ontometrics.util.DateBuilder;
import ontometrics.test.util.TestUtil;
import ontometrics.test.util.UrlResourceProvider;
import org.junit.Before;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static java.util.Calendar.JULY;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class SourceEventMapperTest {

    private static final Logger log = LoggerFactory.getLogger(SourceEventMapperTest.class);

    private URL sourceUrl;
    private URL editsUrl;

    @Before
    public void setup(){
        sourceUrl = TestUtil.getFileAsURL("/feeds/issues-feed-rss.xml");
        editsUrl = TestUtil.getFileAsURL("/feeds/issue-changes.xml");
    }

    @Test
    public void testGettingLocalFileAsUrlWorks(){
        assertThat(sourceUrl, notNullValue());
    }

    @Test
    public void testThatWeCanReadFromFile() throws IOException, XMLStreamException {
        int startElementCount = 0;
        int endElementCount = 0;
        InputStream inputStream = sourceUrl.openStream();
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
        InputStream inputStream = sourceUrl.openStream();
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
    public void testThatWeCanExtractYouTrackEvent() throws IOException, XMLStreamException {
        
        SourceEventMapper sourceEventMapper = new SourceEventMapper(UrlResourceProvider.instance(sourceUrl));
        List<ProcessEvent> events = sourceEventMapper.getLatestEvents();
        
        ProcessEvent firstEvent = events.get(events.size()-1);

        assertThat(firstEvent.getTitle(), is("ASOC-28: User searches for Users by name"));
        assertThat(firstEvent.getLink(), is("http://ontometrics.com:8085/issue/ASOC-28"));
        assertThat(firstEvent.getDescription(), is("<table> <tr> <th>Reporter</th> <td> <img src=\"http://ontometrics.com:8085/_classpath/smartui/img/youPicture.gif\" width=\"56\" height=\"59\" alt=\"Tim Fulmer (timfulmer)\" title=\"Tim Fulmer (timfulmer)\"/> Tim Fulmer (timfulmer) </td> </tr> <tr> <th>Created</th> <td>Apr 15, 2014 9:05:53 AM</td> </tr> <tr> <th>Updated</th> <td>Jul 14, 2014 9:41:03 AM</td> </tr> <tr> <th>Priority</th> <td>Normal</td> </tr> <tr> <th>Type</th> <td>Feature</td> </tr> <tr> <th>State</th> <td>Open</td> </tr> <tr> <th>Assignee</th> <td>Noura Hassan (noura)</td> </tr> <tr> <th>Subsystem</th> <td>No subsystem</td> </tr> <tr> <th>Fix versions</th> <td>1.0.0</td> </tr> <tr> <th>Affected versions</th> <td>Unknown</td> </tr> <tr> <th>Fixed in build</th> <td>Next Build</td> </tr> </table> <div class=\"wiki text\">User can search for other Users by name, first screenshot. Users can follow in the search results. Tapping on a search result shows the selected User&#39;s profile page.<br/><br/>API service call:<br/><br/><a href=\"http://devvixletapi-env.elasticbeanstalk.com/#!/search/_get_0\">http://devvixletapi-env.elasticbeanstalk.com/#!/search/_get_0</a></div>"));

        log.info("size of events: {}", events.size());
        assertThat(events.size(), is(not(0)));
    }

    @Test
    public void testThatLastSeenEventTracked() throws IOException {
        SourceEventMapper sourceEventMapper = new SourceEventMapper(UrlResourceProvider.instance(sourceUrl));
        List<ProcessEvent> events = sourceEventMapper.getLatestEvents();

        assertThat(sourceEventMapper.getLastEvent(), is(events.get(events.size() - 1)));
    }

    @Test
    public void testThatEventsStreamProcessed() throws IOException {

        SourceEventMapper sourceEventMapper = new SourceEventMapper(UrlResourceProvider.instance(sourceUrl));
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
    public void testThatWeCanGetMostRecentChanges() throws IOException {
        SourceEventMapper sourceEventMapper = new SourceEventMapper(UrlResourceProvider.instance(sourceUrl));
        sourceEventMapper.setEditsUrl(editsUrl);
        List<ProcessEventChange> recentChanges = sourceEventMapper.getLatestChanges();

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
    public void testThatLastEventIsCorrectlyUsedToRetrieveLatestEvents() throws ParseException, IOException {
        SourceEventMapper sourceEventMapper = new SourceEventMapper(UrlResourceProvider.instance(sourceUrl));
        sourceEventMapper.setEditsUrl(editsUrl);
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