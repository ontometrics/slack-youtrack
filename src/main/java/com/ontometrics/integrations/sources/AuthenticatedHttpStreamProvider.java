package com.ontometrics.integrations.sources;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;

import java.io.IOException;
import java.net.URL;

/**
 * External http resource stream provider. Http call performed by {@link org.apache.http.client.fluent.Executor} and may be
 * authenticated with {@link com.ontometrics.integrations.sources.Authenticator}
 *
 * ExternalStreamProvider.java
 */
public class AuthenticatedHttpStreamProvider implements StreamProvider {
    private Executor httpExecutor;

    /**
     *
     * @param authenticator instance which will configure this instance to make authenticated requests
     */
    public AuthenticatedHttpStreamProvider(Authenticator authenticator) {
        this.httpExecutor = Executor.newInstance();
        authenticator.authenticate(httpExecutor);
    }


    /**
     * @throws IOException
     */
    @Override
    public <RES> RES openResourceStream(URL resourceUrl, final InputStreamHandler<RES> inputStreamHandler) throws Exception{
//        return httpExecutor.execute(Request.Get(resourceUrl.toExternalForm()))
//                .handleResponse(httpResponse -> {
//                    try {
//                        return inputStreamHandler.handleStream(httpResponse.getEntity().getContent());
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//            );
        return null;
    }
}
