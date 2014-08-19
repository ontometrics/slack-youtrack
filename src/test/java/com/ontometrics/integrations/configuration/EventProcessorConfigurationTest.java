package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.ProcessEvent;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.Before;
import org.junit.Test;

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
    public void testThatLastEventChangeDateIsStored() throws ConfigurationException {
        assertThat(configuration.loadLastProcessedEvent(), nullValue());

        Calendar eventCalendar = Calendar.getInstance();
        eventCalendar.set(Calendar.HOUR_OF_DAY, 17);
        eventCalendar.set(Calendar.YEAR, 2011);
        eventCalendar.set(Calendar.MONTH, 1);
        eventCalendar.set(Calendar.DAY_OF_MONTH, 2);

        Calendar lastEventChangeTime = Calendar.getInstance();
        lastEventChangeTime.set(Calendar.HOUR_OF_DAY, 10);
        lastEventChangeTime.set(Calendar.YEAR, 2012);
        lastEventChangeTime.set(Calendar.MONTH, 7);
        lastEventChangeTime.set(Calendar.DAY_OF_MONTH, 21);

        ProcessEvent event1 = new ProcessEvent.Builder().link("http://ontometrics.com:8085/issue/ASOC-148")
                .published(eventCalendar.getTime()).title("ASOC-148: New Embedding requirement").build();

        configuration.saveEventChangeDate(event1, lastEventChangeTime.getTime());

        //restarting the configuration and database to make sure that even after server restart correct
        // date will be retrieved for event
        configuration.reload();
        Date storedChangeDate = configuration.getEventChangeDate(event1);
        assertThat(storedChangeDate, notNullValue());
        assertThat(storedChangeDate, is(lastEventChangeTime.getTime()));

        ProcessEvent event2 = new ProcessEvent.Builder().link("http://ontometrics.com:8085/issue/ASOC-149")
                .published(eventCalendar.getTime()).title("ASOC-149: New Embedding requirement").build();
        assertThat(configuration.getEventChangeDate(event2), nullValue());

    }
}
