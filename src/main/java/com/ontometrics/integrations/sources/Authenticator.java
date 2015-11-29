package com.ontometrics.integrations.sources;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;

/**
 * Authenticates request issued by {@link com.ontometrics.integrations.sources.SourceEventMapper}
 */
public interface Authenticator {
    Request authenticate(Executor httpExecutor, Request request);
}
