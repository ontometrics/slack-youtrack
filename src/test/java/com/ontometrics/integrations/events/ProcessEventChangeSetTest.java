package com.ontometrics.integrations.events;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProcessEventChangeSetTest {

    @Test
    public void canCreateAChangeSet(){
        Issue issue = new Issue.Builder().projectPrefix("ASOC").id(405).build();

    }

}