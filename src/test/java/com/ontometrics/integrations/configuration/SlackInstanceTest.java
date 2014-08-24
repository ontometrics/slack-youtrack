package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.IssueEditSession;
import com.ontometrics.integrations.events.TestDataFactory;
import com.ontometrics.integrations.sources.ChannelMapper;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

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

        IssueEditSession session = TestDataFactory.build();

        String message = slackInstance.buildSessionMessage(session);

        log.info("message: {}", message);

    }

    @Test
    public void testGetUsers() throws Exception {

    }
}