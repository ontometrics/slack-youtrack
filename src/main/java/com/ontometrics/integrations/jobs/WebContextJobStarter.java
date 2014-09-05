package com.ontometrics.integrations.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Create and schedule tasks on web-application startup with call to {@link JobStarter#scheduleTasks()}
 * WebContextJobStarter.java
 */
public class WebContextJobStarter implements ServletContextListener {
    private static Logger logger = LoggerFactory.getLogger(WebContextJobStarter.class);

    private JobStarter jobStarter;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        logger.info("Started up");
        this.jobStarter = new JobStarter();
        try {
            jobStarter.scheduleTasks();
        } catch (Exception ex) {
            logger.error("Failed to initialize", ex);
            throw ex;
        }
    }


    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        logger.info("Shutting down");
        jobStarter.dispose();
    }
}
