package com.ontometrics.integrations.sources;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;
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
                public Request authenticate(Executor httpExecutor, Request request) {
                    httpExecutor.auth(login,password);
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
        request = this.authenticator.authenticate(httpExecutor, request);
        return httpExecutor.execute(request)
                .handleResponse(
                    new ResponseHandler<RES>() {
                        @Override
                        public RES handleResponse(HttpResponse httpResponse) throws IOException {
                            try {
                                StatusLine statusLine = httpResponse.getStatusLine();
                                if (StringUtils.isNotBlank(statusLine.getReasonPhrase())){
                                    logger.debug("Got response with code {} reason: {}", statusLine.getStatusCode(), statusLine.getReasonPhrase());
                                } else {
                                    logger.debug("Got response with code {}", statusLine.getStatusCode());
                                }
                                return inputStreamHandler.handleStream(httpResponse.getEntity().getContent(), statusLine.getStatusCode());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                );
    }
}
