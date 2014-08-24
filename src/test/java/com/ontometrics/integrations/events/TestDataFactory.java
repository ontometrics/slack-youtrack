package com.ontometrics.integrations.events;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * User: Rob
 * Date: 8/23/14
 * Time: 8:38 PM
 * <p/>
 * (c) ontometrics 2014, All Rights Reserved
 */
public class TestDataFactory {

    public static IssueEditSession build() throws MalformedURLException {
        URL linkUrl = new URL("http://ontometrics.com:8085/issues/ASOC-408");
        Issue issue = new Issue.Builder().projectPrefix("ASOC").id(408)
                .title("ASOC-408: Need to toggle follow button")
                .description("Right now the button does not change from Follow to Unfollow.")
                .link(linkUrl)
                .build();

        IssueEdit edit1 = new IssueEdit.Builder()
                .issue(issue)
                .field("State")
                .priorValue("Assigned")
                .currentValue("Fixed")
                .build();

        IssueEdit edit2 = new IssueEdit.Builder()
                .issue(issue)
                .field("Priority")
                .priorValue("Normal")
                .currentValue("Critical")
                .build();

        List<IssueEdit> edits = new ArrayList<>();
        edits.add(edit1);
        edits.add(edit2);

        return new IssueEditSession.Builder()
                .issue(issue)
                .changes(edits)
                .updated(new Date())
                .updater("Noura")
                .build();

    }

}
