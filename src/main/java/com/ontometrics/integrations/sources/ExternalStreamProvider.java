package com.ontometrics.integrations.sources;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;

import java.io.IOException;

/**
 * External http resource stream provider. Http call performed by {@link org.apache.http.client.fluent.Executor} and may be
 * authenticated with {@link com.ontometrics.integrations.sources.Authenticator}
 *
 * ExternalStreamProvider.java
 */
public class ExternalStreamProvider implements StreamProvider {
    private Executor httpExecutor;

    private String url;

    public ExternalStreamProvider(String url) {
        this.url = url;
        this.httpExecutor = Executor.newInstance();
    }

    /**
     * Authenticates the call before execution of {@link #openResourceStream(InputStreamHandler)}
     * @param authenticator authenticator
     * @return this
     */
    public ExternalStreamProvider authenticator(Authenticator authenticator) {
        authenticator.authenticate(httpExecutor);
        return this;
    }

    /**
     * @throws IOException
     */
    @Override
    public <RES> RES openResourceStream(final InputStreamHandler<RES> inputStreamHandler) throws IOException {
        return httpExecutor.execute(Request.Get(url))
                .handleResponse(httpResponse -> inputStreamHandler.handleStream(httpResponse.getEntity().getContent()));
    }
}
