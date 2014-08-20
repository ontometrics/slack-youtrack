package com.ontometrics.integrations.sources;

import java.io.IOException;
import java.net.URL;

/**
 * Provides a stream to resource handled by {@link com.ontometrics.integrations.sources.InputStreamHandler}
 *
 * StreamProvider.java
 *
 */
public interface StreamProvider {
    /**
     * Opens a resource and provides its {@link java.io.InputStream} in the call to
     * {@link com.ontometrics.integrations.sources.InputStreamHandler#handleStream(java.io.InputStream)}
     * @param resourceUrl url of the resource to be accessed/processed by inputStreamHandler)
     * @param inputStreamHandler resource stream handler
     * @param <RES> class of resource stream handling result
     * @return result
     * @throws IOException in case if source operation failed
     */
    <RES> RES openResourceStream(URL resourceUrl, final InputStreamHandler<RES> inputStreamHandler) throws Exception;
}
