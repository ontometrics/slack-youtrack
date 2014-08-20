package com.ontometrics.integrations.sources;

import java.io.IOException;
import java.io.InputStream;

/**
 * Handler of {@link java.io.InputStream} which produces a result
 *
 */
public interface InputStreamHandler<RES> {
    RES handleStream(InputStream is) throws Exception;
}
