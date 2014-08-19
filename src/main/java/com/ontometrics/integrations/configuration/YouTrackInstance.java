package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.Issue;

/**
 * Created by rob on 8/19/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public class YouTrackInstance {

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

    public String getBaseUrl() {
        return String.format("%s%s", baseUrl, (port > 0) ? ":" + port : "");
    }

    public String getFeedUrl() {
        return String.format("%s/_rss/issues", getBaseUrl());
    }

    public String getChangesUrl(Issue issue){
        return String.format("%s/rest/issue/%s/changes", getBaseUrl(), issue.toString());
    }

}
