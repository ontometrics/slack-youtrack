package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.Issue;
import ontometrics.test.util.TestUtil;

import java.net.URL;

/**
 *  Mock issue tracker
 *
 * MockIssueTracker.java
 */
public class SimpleMockIssueTracker implements IssueTracker {
    private String feedUrl;
    private String changesUrl;

    public SimpleMockIssueTracker(String feedUrl, String changesUrl) {
        this.feedUrl = feedUrl;
        this.changesUrl = changesUrl;
    }

    @Override
    public URL getBaseUrl() {
        return null;
    }

    @Override
    public URL getFeedUrl() {
        return TestUtil.getFileAsURL(feedUrl);
    }

    @Override
    public URL getChangesUrl(Issue issue) {
        return TestUtil.getFileAsURL(changesUrl);
    }
}