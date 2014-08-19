package com.ontometrics.integrations.sources;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;

import java.io.IOException;

/**
 * ExternalResourceInputStreamProvider.java
 */
public class ExternalResourceInputStreamProvider implements InputStreamProvider {
    private Executor httpExecutor;

    private String url;

    public ExternalResourceInputStreamProvider(String url) {
        this.url = url;
        this.httpExecutor = Executor.newInstance();
    }

    public ExternalResourceInputStreamProvider authenticator(Authenticator authenticator) {
        authenticator.authenticate(httpExecutor);
        return this;
    }

    /**
     * @throws IOException
     */
    @Override
    public <RES> RES openStream(final InputStreamHandler<RES> inputStreamHandler) throws IOException {
        return httpExecutor.execute(Request.Get(url))
                .handleResponse(httpResponse -> inputStreamHandler.handleStream(httpResponse.getEntity().getContent()));
    }
}
