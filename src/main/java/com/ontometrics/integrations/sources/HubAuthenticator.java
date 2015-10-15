package com.ontometrics.integrations.sources;

import com.intellij.hub.auth.oauth2.token.AccessToken;
import jetbrains.jetpass.client.hub.HubClient;
import jetbrains.jetpass.client.oauth2.OAuth2Client;
import jetbrains.jetpass.client.oauth2.token.OAuth2ClientFlow;
import org.apache.http.client.fluent.Executor;


/**
 * This use Hub Client Credentials OAuth
 * https://www.jetbrains.com/hub/help/1.0/Client-Credentials.html
 */
public class HubAuthenticator implements Authenticator {


    private String clientServiceId;
    private String clientServiceSecret;
    private String resourceServerServiceId;
    private String hubUrl;

    private AccessToken accessToken;


    public HubAuthenticator(String clientServiceId, String clientServiceSecret, String resourceServerServiceId, String hubUrl) {
        this.clientServiceId = clientServiceId;
        this.clientServiceSecret = clientServiceSecret;
        this.resourceServerServiceId = resourceServerServiceId;
        this.hubUrl = hubUrl;
    }

    @Override
    public void authenticate(Executor httpExecutor) {
        HubClient hubClient = HubClient.builder().baseUrl(hubUrl).build();
        OAuth2Client oauthClient = hubClient.getOAuthClient();

        OAuth2ClientFlow.Builder clientFlowBuilder = oauthClient.clientFlow();

        clientFlowBuilder.clientId(clientServiceId);

        clientFlowBuilder.clientSecret(clientServiceSecret);

        // An id of service that will be accessed, e.g. YouTrack, TeamCity, UpSource, etc.
        clientFlowBuilder.addScope(resourceServerServiceId);

        OAuth2ClientFlow clientFlow = clientFlowBuilder.build();

        this.accessToken = clientFlow.getToken();
    }
}
