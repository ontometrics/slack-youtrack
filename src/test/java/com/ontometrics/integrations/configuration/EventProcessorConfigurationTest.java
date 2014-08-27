package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.ProcessEvent;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.time.DateUtils;
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
    public void setUp() throws ConfigurationException {
        this.configuration = EventProcessorConfiguration.instance();
        configuration.clear();
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


    /**
     * Verifies that last processed date is stored if it is after current one or current one is not defined
     */
    @Test
    public void testThatOnlyDatesAfterTheCurrentLastProcessedDateAreStored() throws ConfigurationException, MalformedURLException {

        Date date_1 =  new Date(10000);
        Date date_2 =  new Date(20000);
        Date date_3 =  new Date(20000);

        assertThat(configuration.loadLastProcessedDate(), nullValue());
        configuration.saveLastProcessedEventDate(date_1);
        assertThat(configuration.loadLastProcessedDate(), is(date_1));
        configuration.reload();
        assertThat(configuration.loadLastProcessedDate(), is(date_1));

        configuration.saveLastProcessedEventDate(date_3);
        assertThat(configuration.loadLastProcessedDate(), is(date_3));
        configuration.reload();
        assertThat(configuration.loadLastProcessedDate(), is(date_3));

        configuration.saveLastProcessedEventDate(date_2);
        assertThat(configuration.loadLastProcessedDate(), is(date_3));
        configuration.reload();
        assertThat(configuration.loadLastProcessedDate(), is(date_3));
    }


    @Test
    public void testThatMinimumAllowedDateCorrectlyResolved(){
        EventProcessorConfiguration configuration = EventProcessorConfiguration.instance();
        ConfigurationFactory.get().setProperty(EventProcessorConfiguration.PROP_ISSUE_HISTORY_WINDOW, "10");

        Date elevenMinutesBefore = DateUtils.addMinutes(new Date(), -11);
        Date eightMinutesBefore = DateUtils.addMinutes(new Date(), -8);
        assertThat(configuration.resolveMinimumAllowedDate(eightMinutesBefore), is(eightMinutesBefore));

        Date tenMinutesBefore = DateUtils.addMinutes(new Date(), -10);
        assertDatesAreAlmostEqual(configuration.resolveMinimumAllowedDate(elevenMinutesBefore), tenMinutesBefore, 10);

        assertDatesAreAlmostEqual(configuration.resolveMinimumAllowedDate(null), tenMinutesBefore, 10);
    }

    private void assertDatesAreAlmostEqual(Date date1, Date date2, int maxDiff) {
        if (Math.abs(date1.getTime() - date2.getTime()) > maxDiff) {
            assertThat("Dates are not equals", date1, is(date2));
        }
    }
}
