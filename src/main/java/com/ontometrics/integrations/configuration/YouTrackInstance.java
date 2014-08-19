package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.Issue;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by rob on 8/19/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public class YouTrackInstance implements IssueTracker {

    private final int port;
    private final String baseUrl;

    public YouTrackInstance(Builder builder) {
        baseUrl = builder.baseUrl;
        port = builder.port;
    }

    public static class Builder {

        private String baseUrl;
        private int port;

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public YouTrackInstance build(){
            return new YouTrackInstance(this);
            }
    }

    @Override
    public URL getBaseUrl() {
        URL url = null;
        try {
            url = new URL(String.format("%s%s", baseUrl, (port > 0) ? ":" + port : ""));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }

        @Override
    public URL getFeedUrl() {
        URL url = null;
        try {
            url = new URL(String.format("%s/_rss/issues", getBaseUrl()));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }

    @Override
    public URL getChangesUrl(Issue issue){
        URL url = null;
        try {
            url = new URL(String.format("%s/rest/issue/%s/changes", getBaseUrl(), issue.toString()));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }

}
