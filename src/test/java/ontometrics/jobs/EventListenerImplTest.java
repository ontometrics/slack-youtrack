package ontometrics.jobs;

import com.ontometrics.integrations.configuration.EventProcessorConfiguration;
import com.ontometrics.integrations.jobs.EventListenerImpl;
import com.ontometrics.integrations.sources.ChannelMapper;
import com.ontometrics.integrations.sources.InputStreamHandler;
import com.ontometrics.integrations.sources.InputStreamProvider;
import com.ontometrics.integrations.events.ProcessEvent;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.Test;

import java.io.IOException;
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
        assertThat(eventListener.getSourceEventMapper(), notNullValue());
        assertThat(eventListener.getSourceEventMapper().getLastEvent(), nullValue());
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
        assertThat(eventListener.getSourceEventMapper(), notNullValue());
        assertThat(eventListener.getSourceEventMapper().getLastEvent(), notNullValue());
        assertThat(eventListener.getSourceEventMapper().getLastEvent().getLink(), is(processEvent.getLink()));
        assertThat(eventListener.getSourceEventMapper().getLastEvent().getPublishDate(), is(processEvent.getPublishDate()));

    }

    private EventListenerImpl createEventListener() {
        return new EventListenerImpl(new InputStreamProvider() {
            @Override
            public <RES> RES openStream(InputStreamHandler<RES> inputStreamHandler) throws IOException {
                return null;
            }
        }, new ChannelMapper.Builder().build());
    }

}
