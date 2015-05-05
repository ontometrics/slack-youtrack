package com.ontometrics.integrations.sources;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
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

    /**
     * @param authenticator instance which will configure this instance to make authenticated requests
     */
    public AuthenticatedHttpStreamProvider(Authenticator authenticator) {
        this.httpExecutor = Executor.newInstance();
        authenticator.authenticate(httpExecutor);
    }

    public static AuthenticatedHttpStreamProvider basicAuthenticatedHttpStreamProvider
            (final String login, final String password) {
        return new AuthenticatedHttpStreamProvider( new Authenticator() {
                @Override
                public void authenticate(Executor httpExecutor) {
                    httpExecutor.auth(login,password);
                }
            }
        );
    }


    /**
     * @throws IOException
     */
    @Override
    public <RES> RES openResourceStream(URL resourceUrl, final InputStreamHandler<RES> inputStreamHandler) throws Exception {
        return httpExecutor.execute(Request.Get(resourceUrl.toExternalForm()))
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
