package com.ontometrics.integrations.jobs;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Launch list of {@link com.ontometrics.integrations.jobs.EventListener} in a timer job on web-app startup
 * WebContextJobStarter.java
 */
public class WebContextJobStarter implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        //TODO start timer here
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        //TODO stop timer here
    }
}
