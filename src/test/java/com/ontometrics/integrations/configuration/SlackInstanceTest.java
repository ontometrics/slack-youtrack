package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.ProcessEventChange;
import com.ontometrics.integrations.sources.ChannelMapper;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.net.URL;

import static org.slf4j.LoggerFactory.getLogger;

public class SlackInstanceTest {

    private Logger log = getLogger(SlackInstanceTest.class);
    private SlackInstance slackInstance;

    @Before
    public void setUp() throws Exception {
        ChannelMapper channelMapper = new ChannelMapper.Builder()
                .addMapping("ASCO", "vixlet")
                .addMapping("DMIN", "dminder")
                .build();

        slackInstance = new SlackInstance.Builder().channelMapper(channelMapper).build();

    }

    @Test
    public void testPost() throws Exception {

        URL linkUrl = new URL("http://ontometrics.com:8085/issues/ASOC-408");
        Issue issue = new Issue.Builder().projectPrefix("ASOC").id(408)
                .title("ASOC-408: Need to toggle follow button")
                .description("Right now the button does not change from Follow to Unfollow.")
                .link(linkUrl)
                .build();

        ProcessEventChange change = new ProcessEventChange.Builder()
                .issue(issue)
                .field("State")
                .priorValue("Assigned")
                .currentValue("Fixed")
                .updater("Noura")
                .build();

        String message = slackInstance.buildChangeMessage(change);

        log.info("message: {}", message);

    }

    @Test
    public void testGetUsers() throws Exception {

    }
}