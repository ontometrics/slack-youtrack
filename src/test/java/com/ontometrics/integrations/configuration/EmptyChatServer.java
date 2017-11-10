package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.IssueEditSession;
import com.ontometrics.integrations.sources.ChannelMapper;

/**
 * ChatServer with no operations
 *
 * EmptyChatServer.java
 */
public class EmptyChatServer implements ChatServer {
    @Override
    public void postIssueCreation(Issue issue) {

    }

    @Override
    public void post(IssueEditSession issueEditSession) {

    }

    @Override
    public ChannelMapper getChannelMapper() {
        return null;
    }
}
