package com.ontometrics.integrations.sources;

import java.io.IOException;
import java.io.InputStream;

/**
 * InputStreamProvider.java
 *
 */
public interface InputStreamProvider {
    InputStream openStream() throws IOException;
}
