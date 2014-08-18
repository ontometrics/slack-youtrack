package com.ontometrics.integrations.sources;

import org.apache.http.client.fluent.Executor;

/**
 * Authenticates request issued by {@link com.ontometrics.integrations.sources.SourceEventMapper}
 */
public interface Authenticator {
    void authenticate(Executor httpExecutor);
}
