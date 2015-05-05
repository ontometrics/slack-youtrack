package com.ontometrics.integrations.sources;

import com.ontometrics.integrations.configuration.ConfigurationFactory;
import com.ontometrics.integrations.configuration.YouTrackInstance;
import com.ontometrics.integrations.events.IssueEditSession;
import com.ontometrics.integrations.events.ProcessEvent;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * This test check that code can access real server. it is disbaled by default, if you want to run it, please make sure that
 * application.propertied has correct values for following properties
 * <ul>
 * <li>PROP.YOUTRACK_USERNAME</li>
 * <li>PROP.YOUTRACK_PASSWORD</li>
 * <li>PROP.YOUTRACK_URL</li>
 * </ul>
 */
@Ignore
public class EditSessionsExtractorIntegrationTest {
    private static final Logger log = getLogger(EditSessionsExtractorIntegrationTest.class);


    private YouTrackInstance youTrackInstance;
    private StreamProvider streamProvider;

    @Before
    public void setup(){
        Configuration configuration = ConfigurationFactory.get();

        streamProvider = AuthenticatedHttpStreamProvider.basicAuthenticatedHttpStreamProvider(
                configuration.getString("PROP.YOUTRACK_USERNAME"),
                configuration.getString("PROP.YOUTRACK_PASSWORD")
        );

        youTrackInstance = new YouTrackInstance.Builder().baseUrl(
                configuration.getString("PROP.YOUTRACK_URL")).build();
    }

    @Test
    public void testThatWeCanGetEventsFromRealFeed() throws Exception {
        EditSessionsExtractor sourceEventMapper = new EditSessionsExtractor(youTrackInstance, streamProvider);
        List<ProcessEvent> changes = sourceEventMapper.getLatestEvents();
        assertThat(changes, not(empty()));
    }

    @Test
    public void testThatWeCanGetEditSessionsFromRealFeed() throws Exception {
        EditSessionsExtractor sourceEventMapper = new EditSessionsExtractor(youTrackInstance, streamProvider);

        //NOTE: min date has been set to "1 day before" to minimize the number of requests
        //adjust it, if yours server has no data within that period
        Date minDate = new Date(System.currentTimeMillis() - 24*3600*1000);

        List<IssueEditSession> edits = sourceEventMapper.getLatestEdits(minDate);
        assertThat(edits, not(empty()));
        log.info("found {} edits: {}", edits.size(), edits);
    }


}