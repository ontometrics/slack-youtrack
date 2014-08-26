package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.ProcessEvent;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

/**
 * EventProcessorConfiguration.java
 * Organize access (read/write) to properties/state required for processing of input/output streams
 *
 */
public class EventProcessorConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(EventProcessorConfiguration.class);

    private static final EventProcessorConfiguration instance = new EventProcessorConfiguration();

    private static final String LAST_EVENT_LINK = "last.event.link";
    private static final String LAST_EVENT_PUBLISHED = "last.event.published";
    private static final String LAST_EVENT_TITLE = "last.event.title";
    public static final String EVENT_CHANGE_DATES = "eventChangeDates";
    private static final String DEPLOYMENT_TIME = "deploymentDate";

    private PropertiesConfiguration lastEventConfiguration;
    private DB db;
    private BTreeMap<String, Long> eventChangeDatesCollection;

    private EventProcessorConfiguration() {
        initialize();
    }

    private void initialize() throws ConfigurationAccessError {
        try {
            File dataDir = new File(ConfigurationFactory.get().getString("PROP.APP_DATA_DIR", "."));
            File file = new File(dataDir, "lastEvent.properties");
            logger.info("Going to load properties from file {}", file.getAbsolutePath());
            lastEventConfiguration = new PropertiesConfiguration(file);
            if (!lastEventConfiguration.containsKey(DEPLOYMENT_TIME)) {
                lastEventConfiguration.setProperty(DEPLOYMENT_TIME, System.currentTimeMillis());
                lastEventConfiguration.save();
            }
            db = DBMaker.newFileDB(new File(dataDir, "app_db")).closeOnJvmShutdown().make();
            eventChangeDatesCollection = getEventChangeDatesCollection();
            logger.info("Initialized EventProcessorConfiguration");
        } catch (ConfigurationException e) {
            throw new ConfigurationAccessError("Failed to access properties", e);
        }
    }

    public static EventProcessorConfiguration instance() {
        return instance;
    }

    /**
     * @return last event processed (issue) or null if not available
     */
    public ProcessEvent loadLastProcessedEvent() {
        String lastEventLink = lastEventConfiguration.getString(LAST_EVENT_LINK);
        String lastEventTitle = lastEventConfiguration.getString(LAST_EVENT_TITLE, null);
        long published= lastEventConfiguration.getLong(LAST_EVENT_PUBLISHED, 0);
        if (lastEventLink != null && published > 0) {
            try {
                String i = lastEventLink.substring(lastEventLink.lastIndexOf("/") + 1);
                String[] iParts = i.split("-");
                String prefix = iParts[0];
                int id = Integer.parseInt(iParts[1]);
                Issue issue = new Issue.Builder().projectPrefix(prefix).id(id).title(lastEventTitle).link(new URL(lastEventLink)).build();
                return new ProcessEvent.Builder().issue(issue).published(new Date(published)).build();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Saves last processed event change date to database
     * @param event event
     * @param date processed event change date
     */
    public void saveEventChangeDate(ProcessEvent event, Date date) {
        eventChangeDatesCollection.put(event.getIssue().toString(), date.getTime());
        db.commit();

    }

    private BTreeMap<String, Long> getEventChangeDatesCollection() {
        return db.getTreeMap(EVENT_CHANGE_DATES);
    }

    public Date getEventChangeDate(ProcessEvent event) {
        Long date = eventChangeDatesCollection.get(event.getIssue().toString());
        return date == null ? null : new Date(date);
    }

    public void saveLastProcessEvent(ProcessEvent processEvent) throws ConfigurationException {
        lastEventConfiguration.setProperty(LAST_EVENT_LINK, processEvent.getIssue().getLink().toExternalForm());
        lastEventConfiguration.setProperty(LAST_EVENT_PUBLISHED, processEvent.getPublishDate().getTime());
        lastEventConfiguration.setProperty(LAST_EVENT_TITLE, processEvent.getIssue().getTitle());
        lastEventConfiguration.save();
    }

    public void clearLastProcessEvent() throws ConfigurationException {
        lastEventConfiguration.clearProperty(LAST_EVENT_LINK);
        lastEventConfiguration.clearProperty(LAST_EVENT_PUBLISHED);
        lastEventConfiguration.clearProperty(LAST_EVENT_TITLE);
        lastEventConfiguration.save();
    }

    /**
     * Date when application when first deployed. Only events with date more than deployment date will be reported to
     * Slack
     * @return deployment date
     */
    public Date getDeploymentTime() {
        Long deploymentDate = lastEventConfiguration.getLong(DEPLOYMENT_TIME, null);
        return deploymentDate == null ? new Date() : new Date(deploymentDate);
    }

    public void setDeploymentTime(Date deploymentDate) throws ConfigurationException {
        lastEventConfiguration.setProperty(DEPLOYMENT_TIME, deploymentDate.getTime());
        lastEventConfiguration.save();
    }

    /**
     * Releases resource: closes database
     */
    public void dispose() {
        logger.info("Disposing database");
        db.close();
    }

    /**
     * Disposes itself closing the {@link #db} and re-initializes
     */
    public void reload() {
        dispose();
        initialize();
    }
}
