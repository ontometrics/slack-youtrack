package com.ontometrics.integrations.configuration;

import org.apache.commons.configuration.Configuration;

public class YouTrackInstanceFactory {
    private static final String YT_FEED_URL = "https://issuetracker.ontometrics.com";

    public static YouTrackInstance createYouTrackInstance(Configuration configuration) {
        return new YouTrackInstance.Builder().baseUrl(
                configuration.getString("PROP.YOUTRACK_URL", YT_FEED_URL))
                .externalBaseUrl(
                        configuration.getString("PROP.YOUTRACK_EXTERNAL_URL", YT_FEED_URL)).build();
    }

}
