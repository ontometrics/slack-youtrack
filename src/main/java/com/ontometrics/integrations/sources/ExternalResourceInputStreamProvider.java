package com.ontometrics.integrations.sources;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;

import java.io.IOException;
import java.io.InputStream;

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

    @Override
    public InputStream openStream() throws IOException {
        return httpExecutor.execute(Request.Get(url)).returnContent().asStream();
    }
}
