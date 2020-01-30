package com.ontometrics.integrations.sources;

import org.apache.http.HttpHost;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

/**
 * External http resource stream provider. Http call performed by {@link org.apache.http.client.fluent.Executor} and may be
 * authenticated with {@link com.ontometrics.integrations.sources.Authenticator}
 * <p>
 * ExternalStreamProvider.java
 */
public class AuthenticatedHttpStreamProvider implements StreamProvider {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticatedHttpStreamProvider.class);

    private Executor httpExecutor;
    private Authenticator authenticator;
    /**
     * @param authenticator instance which will configure this instance to make authenticated requests
     */
    public AuthenticatedHttpStreamProvider(Authenticator authenticator) {
        this.httpExecutor = Executor.newInstance();
        this.authenticator = authenticator;
    }

    public static AuthenticatedHttpStreamProvider basicAuthenticatedHttpStreamProvider
            (final String login, final String password) {
        return new AuthenticatedHttpStreamProvider( new Authenticator() {
                @Override
                public Request authenticate(URL resourceUrl, Executor httpExecutor, Request request) {
                    httpExecutor.auth(login,password);
                    httpExecutor.authPreemptive(new HttpHost(resourceUrl.getHost(), resourceUrl.getPort(), resourceUrl.getProtocol()));
                    return request;
                }
            }
        );
    }

    public static AuthenticatedHttpStreamProvider hubAuthenticatedHttpStreamProvider
            (String clientServiceId, String clientServiceSecret, String resourceServerServiceId, String hubUrl) {
        return new AuthenticatedHttpStreamProvider(
                new HubAuthenticator(clientServiceId, clientServiceSecret, resourceServerServiceId, hubUrl));
    }

    /**
     * @throws IOException
     */
    @Override
    public <RES> RES openResourceStream(URL resourceUrl, final InputStreamHandler<RES> inputStreamHandler) throws Exception {
        Request request = Request.Get(resourceUrl.toExternalForm());
        request = this.authenticator.authenticate(resourceUrl, httpExecutor, request);
        return NonAuthenticatedHttpStreamProvider.openResourceStream(request, inputStreamHandler, httpExecutor);
    }
}
