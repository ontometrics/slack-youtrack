package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.sources.AuthenticatedHttpStreamProvider;
import com.ontometrics.integrations.sources.StreamProvider;
import org.apache.commons.configuration.Configuration;

public class StreamProviderFactory {

    private static final String CREDENTIALS_AUTH_TYPE = "credentials";

    public static StreamProvider createStreamProvider(Configuration configuration) {
        if (configuration.getString("PROP.AUTH_TYPE", CREDENTIALS_AUTH_TYPE).equalsIgnoreCase(CREDENTIALS_AUTH_TYPE)) {
            return AuthenticatedHttpStreamProvider.basicAuthenticatedHttpStreamProvider(
                    configuration.getString("PROP.YOUTRACK_USERNAME"), configuration.getString("PROP.YOUTRACK_PASSWORD")
            );
        }

        return AuthenticatedHttpStreamProvider.hubAuthenticatedHttpStreamProvider(
//                6b6adb20-e08b-41c1-bbc9-008720a2198a
                configuration.getString("PROP.HUB_OAUTH_CLIENT_SERVICE_ID"),
//oX8ryJNXwCK0
                configuration.getString("PROP.HUB_OAUTH_CLIENT_SERVICE_SECRET"),
//                6b6adb20-e08b-41c1-bbc9-008720a2198a
                configuration.getString("PROP.HUB_OAUTH_RESOURCE_SERVER_SERVICE_ID"),
//https://hub.ontometrics.com/hub
                configuration.getString("PROP.HUB_URL")
        );
    }


}
