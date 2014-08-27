package com.ontometrics.integrations.events;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import org.junit.Test;

import java.net.MalformedURLException;

public class IssueEditSessionTest {

    @Test
    public void canBuildSession() throws MalformedURLException {

        IssueEditSession issueEditSession = TestDataFactory.build();

        assertThat(issueEditSession.getChanges(), hasSize(2));
        assertThat(issueEditSession.getUpdater(), is("Noura"));

    }

}