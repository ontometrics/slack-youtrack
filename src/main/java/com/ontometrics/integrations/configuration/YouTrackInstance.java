package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.Issue;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by rob on 8/19/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public class YouTrackInstance implements IssueTracker {

    private Logger log = getLogger(YouTrackInstance.class);
    private final String baseUrl;
    private final String issueBase;

    public YouTrackInstance(Builder builder) {
        baseUrl = builder.baseUrl;
        issueBase = getBaseUrl() + "/rest/issue/%s";
    }

    public String getIssueBaseURL(Issue issue) {
        return String.format(issueBase, issue.getPrefix() + "-" + issue.getId());
    }

    public static class Builder {

        private String baseUrl;

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public YouTrackInstance build(){
            return new YouTrackInstance(this);
            }
    }

    @Override
    public URL getBaseUrl() {
        URL url;
        try {
            url = new URL(baseUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return url;
    }

        @Override
    public URL getFeedUrl() {
        URL url;
        try {
            url = new URL(String.format("%s/_rss/issues", getBaseUrl()));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return url;
    }

    @Override
    public URL getChangesUrl(Issue issue){
        URL url;
        try {
            url = new URL(String.format("%s/rest/issue/%s/changes", getBaseUrl(), issue.getPrefix() + "-" + issue.getId()));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return url;
    }

    @Override
    public URL getAttachmentsUrl(Issue issue) {
        return buildIssueURL(issue, "%s/attachment");
    }

    private URL buildIssueURL(Issue issue, String urlTemplate) {
        URL url = null;
        try {
            String base = getIssueBaseURL(issue);
            url = new URL(urlTemplate.replace("%s", base));
        } catch (MalformedURLException e) {
            log.error("Error building issue URL", e);
        }
        return url;
    }

}
