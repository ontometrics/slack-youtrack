package com.ontometrics.integrations.events;

import org.junit.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ProcessEventChangeTest {

    @Test
    public void testThatWeCanBuild(){
        Issue issue = new Issue.Builder()
                .projectPrefix("ASOC")
                .id(408)
                .title("ProcessEvents need to be persisted")
                .description("Right now we are pulling them from the stream, we have to save them.")
                .build();
        ProcessEventChange change = new ProcessEventChange.Builder()
                .issue(issue)
                .field("State")
                .priorValue("Assigned")
                .currentValue("Fixed")
                .updated(new Date())
                .updater("Noura")
                .build();

        assertThat(change.getIssue().getId(), is(408));

    }

}