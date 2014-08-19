package com.ontometrics.integrations.jobs;

import java.io.IOException;

/**
 * Created on 8/18/14.
 */
public interface EventListener {

    public int checkForNewEvents() throws IOException;

}
