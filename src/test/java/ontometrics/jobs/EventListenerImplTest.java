package ontometrics.jobs;

import com.ontometrics.integrations.configuration.EventProcessorConfiguration;
import com.ontometrics.integrations.configuration.SlackInstance;
import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.ProcessEvent;
import com.ontometrics.integrations.jobs.EventListenerImpl;
import com.ontometrics.integrations.sources.ChannelMapper;
import ontometrics.test.util.UrlStreamProvider;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

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
        configuration.saveLastProcessEvent(processEvent);

        EventListenerImpl eventListener = createEventListener();
        assertThat(eventListener.getEditSessionsExtractor(), notNullValue());
        assertThat(eventListener.getEditSessionsExtractor().getLastEvent(), notNullValue());
        assertThat(eventListener.getEditSessionsExtractor().getLastEvent().getIssue().getLink(), is(processEvent.getIssue().getLink()));
        assertThat(eventListener.getEditSessionsExtractor().getLastEvent().getPublishDate(), is(processEvent.getPublishDate()));

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
