package com.ontometrics.integrations.configuration;

import com.ontometrics.util.DateBuilder;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;

/**
 * EventProcessorConfiguration.java
 * Organize access (read/write) to properties/state required for processing of input/output streams
 *
 */
public class EventProcessorConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(EventProcessorConfiguration.class);

    private static final EventProcessorConfiguration instance = new EventProcessorConfiguration();

    private static final String LAST_EVENT_DATE = "last.event.date";

    public static final String PROP_ISSUE_HISTORY_WINDOW = "PROP.ISSUE_HISTORY_WINDOW";

    private PropertiesConfiguration lastEventConfiguration;

    //being used in tests to override value from properties
    private Integer issueHistoryWindowInMinutes;

    private EventProcessorConfiguration() {
        initialize();
    }

    private void initialize() throws ConfigurationAccessError {
        try {
            File dataDir = new File(ConfigurationFactory.get().getString("PROP.APP_DATA_DIR", "."));
            File file = new File(dataDir, "lastEvent.properties");
            logger.info("Going to load properties from file {}", file.getAbsolutePath());
            lastEventConfiguration = new PropertiesConfiguration(file);
            logger.info("Initialized EventProcessorConfiguration");
        } catch (ConfigurationException e) {
            throw new ConfigurationAccessError("Failed to access properties", e);
        }
    }


    public static EventProcessorConfiguration instance() {
        return instance;
    }

    /**
     * @return last event processed (issue) or null if not available for specified project
     */
    public Date loadLastProcessedDate(String project) {
        Long lastEventDate = lastEventConfiguration.getLong(getProjectLastEventDateKey(project), null);
        if (lastEventDate != null && lastEventDate > 0) {
            return new Date(lastEventDate);
        }
        return null;
    }



    public void saveLastProcessedEventDate(Date lastProcessedEventDate, String project) throws ConfigurationException {
        Date currentLastProcessedDate = loadLastProcessedDate(project);
        if (currentLastProcessedDate  == null || currentLastProcessedDate.before(lastProcessedEventDate)) {
            lastEventConfiguration.setProperty(getProjectLastEventDateKey(project), lastProcessedEventDate.getTime());
            lastEventConfiguration.save();
        }
    }

    private String getProjectLastEventDateKey(String project) {
        return LAST_EVENT_DATE+"."+project;
    }

    /**
     * "maximum-allowed window" defined/configured by the configuration property PROP.ISSUE_HISTORY_WINDOW
     * @param date date
     * @return date if it is after the "maximum-allowed window" or date which define the lower bound of "maximum-allowed window"
     */
    public Date resolveMinimumAllowedDate(Date date) {
        Date oldestDateInThePast = oldestDateInThePast();
        if (date == null) {
            return oldestDateInThePast;
        } else if (date.before(oldestDateInThePast)) {
            return oldestDateInThePast;
        }
        return date;
    }

    /**
     * @return Date in the past - N minutes before now. Where N - defined by the property "PROP.ISSUE_HISTORY_WINDOW"
     */
    public Date oldestDateInThePast() {
        return new DateBuilder().addMinutes(-getIssueHistoryWindowInMinutes()).build();
    }

    public void clear() throws ConfigurationException {
        lastEventConfiguration.clear();
        lastEventConfiguration.save();
    }

    public int getIssueHistoryWindowInMinutes() {
        //3 days by default
        if (issueHistoryWindowInMinutes == null) {
            return ConfigurationFactory.get().getInt(PROP_ISSUE_HISTORY_WINDOW, 60 * 24 * 3);
        }
        return issueHistoryWindowInMinutes;
    }

    /**
     * Disposes itself and re-initializes
     */
    public void reload() {
        initialize();
    }

    public void setIssueHistoryWindowInMinutes(int issueHistoryWindowInMinutes) {
        this.issueHistoryWindowInMinutes = issueHistoryWindowInMinutes;
    }
}
