package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.ProcessEvent;
import com.ontometrics.util.DateBuilder;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
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
    public static final String EVENT_CHANGE_DATES = "eventChangeDates";
    public static final String PROP_ISSUE_HISTORY_WINDOW = "PROP.ISSUE_HISTORY_WINDOW";

    private PropertiesConfiguration lastEventConfiguration;
    private DB db;
    private BTreeMap<String, Long> eventChangeDatesCollection;

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
            db = DBMaker.newFileDB(new File(dataDir, "app_db")).closeOnJvmShutdown().make();
            eventChangeDatesCollection = getEventChangeDatesCollection();
            logDatabase();
            logger.info("Initialized EventProcessorConfiguration");
        } catch (ConfigurationException e) {
            throw new ConfigurationAccessError("Failed to access properties", e);
        }
    }

    private void logDatabase() {
        StringBuilder builder = new StringBuilder("Last Event Change keyset:\n");
        for (String key: eventChangeDatesCollection.keySet()) {
            Long value = eventChangeDatesCollection.get(key);
            builder.append("Issue Key: ").append(key).append(", Last Change: ").append(value).append(". ")
                    .append(new Date(value)).append("\n");
        }
        logger.info(builder.toString());
    }

    public static EventProcessorConfiguration instance() {
        return instance;
    }

    /**
     * @return last event processed (issue) or null if not available
     */
    public Date loadLastProcessedDate() {
        Long lastEventDate = lastEventConfiguration.getLong(LAST_EVENT_DATE, null);
        if (lastEventDate != null && lastEventDate > 0) {
            return new Date(lastEventDate);
        }
        return null;
    }

    /**
     * @return last event processed (issue) or null if not available for specified project
     */
    public Date loadLastProcessedDate(String project) {
        Long lastEventDate = lastEventConfiguration.getLong(getProjectLasteventDateKey(project), null);
        if (lastEventDate != null && lastEventDate > 0) {
            return new Date(lastEventDate);
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

    public void saveLastProcessedEventDate(Date lastProcessedEventDate) throws ConfigurationException {
        Date currentLastProcessedDate = loadLastProcessedDate();
        if (currentLastProcessedDate  == null || currentLastProcessedDate.before(lastProcessedEventDate)) {
            lastEventConfiguration.setProperty(LAST_EVENT_DATE, lastProcessedEventDate.getTime());
            lastEventConfiguration.save();
        }
    }

    public void saveLastProcessedEventDate(Date lastProcessedEventDate, String project) throws ConfigurationException {
        Date currentLastProcessedDate = loadLastProcessedDate(project);
        if (currentLastProcessedDate  == null || currentLastProcessedDate.before(lastProcessedEventDate)) {
            lastEventConfiguration.setProperty(getProjectLasteventDateKey(project), lastProcessedEventDate.getTime());
            lastEventConfiguration.save();
        }
    }

    private String getProjectLasteventDateKey(String project) {
        return LAST_EVENT_DATE+"."+project;
    }

    /**
     * "maximum-allowed window" defined/configured by the configuration property PROP.ISSUE_HISTORY_WINDOW
     * @param date date
     * @return date if it is after the "maximum-allowed window" or date which define the lower bound of "maximum-allowed window"
     */
    public Date resolveMinimumAllowedDate(Date date) {
        Date oldestDateInThePast = EventProcessorConfiguration.instance().oldestDateInThePast();
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

    public void clearLastProcessEvent() throws ConfigurationException {
        lastEventConfiguration.clearProperty(LAST_EVENT_DATE);
        lastEventConfiguration.save();
    }

    public void clear() throws ConfigurationException {
        lastEventConfiguration.clear();
        lastEventConfiguration.save();
        eventChangeDatesCollection.clear();
        db.commit();
    }

    public int getIssueHistoryWindowInMinutes() {
        //3 days by default
        if (issueHistoryWindowInMinutes == null) {
            return ConfigurationFactory.get().getInt(PROP_ISSUE_HISTORY_WINDOW, 60 * 24 * 3);
        }
        return issueHistoryWindowInMinutes;
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

    public void setIssueHistoryWindowInMinutes(int issueHistoryWindowInMinutes) {
        this.issueHistoryWindowInMinutes = issueHistoryWindowInMinutes;
    }
}
