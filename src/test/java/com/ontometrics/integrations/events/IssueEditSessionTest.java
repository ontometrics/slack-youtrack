package com.ontometrics.integrations.events;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

import com.ontometrics.util.DateBuilder;
import org.junit.Test;

import java.net.MalformedURLException;
import java.util.Date;

public class IssueEditSessionTest {

    @Test
    public void canBuildSession() throws MalformedURLException {

        IssueEditSession issueEditSession = TestDataFactory.build();

        assertThat(issueEditSession.getChanges(), hasSize(2));
        assertThat(issueEditSession.getUpdater(), is("Noura"));

    }

    @Test
    public void canDetectNewTickets() {
        Date justNow = new Date();
        Date threeMinutesAgo = new DateBuilder().start(justNow).addMinutes(-3).build();
        Issue issue = new Issue.Builder().projectPrefix("AIA").id(721).creator("Rob").created(threeMinutesAgo).build();
        IssueEditSession issueEditSession = new IssueEditSession.Builder().issue(issue).updated(justNow).build();

        assertThat(issueEditSession.isCreationEdit(), is(true));
    }

}