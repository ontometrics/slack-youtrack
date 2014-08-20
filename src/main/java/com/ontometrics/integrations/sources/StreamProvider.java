package com.ontometrics.integrations.sources;

import java.io.IOException;

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
     * @param inputStreamHandler resource stream handler
     * @param <RES> class of resource stream handling result
     * @return result
     * @throws IOException in case if source operation failed
     */
    <RES> RES openResourceStream(final InputStreamHandler<RES> inputStreamHandler) throws IOException;
}
