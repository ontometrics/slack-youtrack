package ontometrics.jobs;

import com.ontometrics.integrations.configuration.EventProcessorConfiguration;
import com.ontometrics.integrations.configuration.SlackInstance;
import com.ontometrics.integrations.events.ProcessEvent;
import com.ontometrics.integrations.jobs.EventListenerImpl;
import com.ontometrics.integrations.sources.ChannelMapperFactory;
import ontometrics.test.util.UrlStreamProvider;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.Test;

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
    public void testThatSourceEventMapperCorrectlyInitializedWithExistingConfiguration() throws ConfigurationException {

        EventProcessorConfiguration configuration = EventProcessorConfiguration.instance();
        ProcessEvent processEvent = new ProcessEvent.Builder()
                .title("ASOC-28: title")
                .link("http://ontometrics.com:8085/issue/ASOC-28")
                .published(new Date()).build();
        configuration.saveLastProcessEvent(processEvent);

        EventListenerImpl eventListener = createEventListener();
        assertThat(eventListener.getEditSessionsExtractor(), notNullValue());
        assertThat(eventListener.getEditSessionsExtractor().getLastEvent(), notNullValue());
        assertThat(eventListener.getEditSessionsExtractor().getLastEvent().getLink(), is(processEvent.getLink()));
        assertThat(eventListener.getEditSessionsExtractor().getLastEvent().getPublishDate(), is(processEvent.getPublishDate()));

    }

    private EventListenerImpl createEventListener() {
        return new EventListenerImpl(UrlStreamProvider.instance(), new SlackInstance.Builder()
                .channelMapper(ChannelMapperFactory.createChannelMapper()).build());
    }

}
