package com.ontometrics.integrations.sources;

import java.io.IOException;
import java.io.InputStream;

/**
 * InputStreamProvider.java
 *
 */
public interface InputStreamProvider {
    <RES> RES openStream(final InputStreamHandler<RES> inputStreamHandler) throws IOException;
}
