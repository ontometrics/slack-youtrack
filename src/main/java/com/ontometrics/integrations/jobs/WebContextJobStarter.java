package com.ontometrics.integrations.jobs;

import com.ontometrics.db.MapDb;
import com.ontometrics.integrations.configuration.ConfigurationFactory;
import com.ontometrics.integrations.configuration.EventProcessorConfiguration;
import com.ontometrics.integrations.configuration.YouTrackInstance;
import com.ontometrics.integrations.configuration.YouTrackInstanceFactory;
import org.apache.commons.configuration.Configuration;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Date;

/**
 * Create and schedule tasks on web-application startup with call to {@link JobStarter#scheduleTasks()}
 * WebContextJobStarter.java
 */
public class WebContextJobStarter implements ServletContextListener {
    private static Logger logger = LoggerFactory.getLogger(WebContextJobStarter.class);

    private JobStarter jobStarter;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        logger.info("Starting up, checking that configuration is correct");
        try {
            checkConfiguration();
            this.jobStarter = new JobStarter();
            jobStarter.scheduleTasks();
        } catch (Exception ex) {
            logger.error("Failed to initialize application", ex);
            throw ex;
        }
    }

    private void checkConfiguration() throws InvalidConfigurationException {
        Configuration configuration = ConfigurationFactory.get();
        try {
            EventProcessorConfiguration.instance();
        } catch (Exception ex) {
            throw new InvalidConfigurationException("Could not initialize event processor configuration", ex);
        }
        YouTrackInstance youTrackInstance = null;
        try {
            youTrackInstance = YouTrackInstanceFactory.createYouTrackInstance(configuration);
        } catch (Exception ex) {
            throw new InvalidConfigurationException("Invalid YouTrack configuration, please check that URL is correct", ex);
        }
        try {
            youTrackInstance.getBaseUrl();
        } catch (Exception ex) {
            throw new InvalidConfigurationException("Invalid YouTrack base url", ex);
        }
        try {
            youTrackInstance.getFeedUrl("TEST", new Date());
        } catch (Exception ex) {
            throw new InvalidConfigurationException("Invalid YouTrack feed url", ex);
        }
    }


    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        logger.info("Shutting down");
        jobStarter.dispose();
        MapDb.instance().close();
    }
}
