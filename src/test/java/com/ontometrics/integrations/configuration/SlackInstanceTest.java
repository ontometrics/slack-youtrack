package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.IssueEditSession;
import com.ontometrics.integrations.events.TestDataFactory;
import com.ontometrics.integrations.sources.ChannelMapper;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

public class SlackInstanceTest {

    private Logger log = getLogger(SlackInstanceTest.class);
    private SlackInstance slackInstance;

    @Before
    public void setUp() throws Exception {
        ChannelMapper channelMapper = new ChannelMapper.Builder()
                .addMapping("ASOC", "vixlet")
                .addMapping("DMIN", "dminder")
                .addMapping("DMAN", "dminder")
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
    public void testNewIssueMessage() throws MalformedURLException {
        Issue issue = new Issue.Builder()
                .projectPrefix("ASOC")
                .id(492)
                .title("ASOC-492 Title autosuggest and normalization")
                .created(new Date())
                .creator("Noura")
                .link(new URL("http://ontometrics.com:8085/issue/ASOC-408"))
                .build();
        String newIssueMessage = slackInstance.buildNewIssueMessage(issue);

        log.info("new issue: {}", newIssueMessage);

        assertThat(newIssueMessage, is("*Noura* created <http://ontometrics.com:8085/issue/ASOC-408|ASOC-492>Title autosuggest and normalization"));

    }

    @Test
    public void testGetUsers() throws Exception {

    }
}