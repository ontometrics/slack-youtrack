package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.IssueEditSession;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Chat server which will fail test if it will got any duplicates messages
 * TrackingChatServer.java
 */
public class CheckDuplicateMessagesChatServer implements ChatServer {

    private List<Issue> createdIssues = new ArrayList<>();
    private List<IssueEditSession> postedIssueEditSessions = new ArrayList<>();

    @Override
    public void postIssueCreation(Issue issue) {
        if (createdIssues.contains(issue)) {
            createdIssues.add(issue);
        } else {
            Assert.fail("Issue "+issue+" has been reported as created before");
        }
    }

    @Override
    public void post(IssueEditSession issueEditSession) {
        if (postedIssueEditSessions.contains(issueEditSession)) {
            postedIssueEditSessions.add(issueEditSession);
        } else {
            Assert.fail("IssueEditSession "+issueEditSession+" has been reported before");
        }

    }
}
