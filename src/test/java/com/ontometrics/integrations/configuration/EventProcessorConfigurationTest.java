package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.ProcessEvent;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link com.ontometrics.integrations.configuration.EventProcessorConfiguration}
 * EventProcessorConfigurationTest.java
 */
public class EventProcessorConfigurationTest {
    private EventProcessorConfiguration configuration;

    @Before
    public void setUp() {
        this.configuration = EventProcessorConfiguration.instance();
    }

    /**
     * Verifies that last event change date is stored (even after database is restarted)
     */
    @Test
    public void testThatLastEventChangeDateIsStored() throws ConfigurationException, MalformedURLException {
//        configuration.clearLastProcessEvent();
//        assertThat(configuration.loadLastProcessedEvent(), nullValue());


        Calendar lastEventChangeTime = Calendar.getInstance();
        lastEventChangeTime.add(Calendar.MINUTE, -2);

        Issue issue = new Issue.Builder().projectPrefix("ASOC").id(148)
                .link(new URL("http://ontometrics.com:8085/issue/ASOC-148"))
                .title("ASOC-148: New Embedding requirement")
                .build();

        ProcessEvent event1 = new ProcessEvent.Builder()
                .issue(issue)
                .published(new Date())
                .build();

        configuration.saveEventChangeDate(event1, lastEventChangeTime.getTime());

        //restarting the configuration and database to make sure that even after server restart correct
        // date will be retrieved for event
        configuration.reload();
        Date storedChangeDate = configuration.getEventChangeDate(event1);
        assertThat(storedChangeDate, notNullValue());
        assertThat(storedChangeDate, is(lastEventChangeTime.getTime()));

        Issue issue2 = new Issue.Builder().projectPrefix("ASOC").id(149)
                .link(new URL("http://ontometrics.com:8085/issue/ASOC-149"))
                .title("ASOC-149: Newer Embedding requirement")
                .build();


        ProcessEvent event2 = new ProcessEvent.Builder()
                .issue(issue2)
                .published(new Date())
                .build();
        assertThat(configuration.getEventChangeDate(event2), nullValue());

    }
}
