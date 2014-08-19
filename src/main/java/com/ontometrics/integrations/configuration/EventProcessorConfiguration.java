package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.ProcessEvent;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.util.Date;

/**
 * EventProcessorConfiguration.java
 * Organize access (read/write) to properties/state required for processing of input/output streams
 *
 */
public class EventProcessorConfiguration {
    private static final EventProcessorConfiguration instance = new EventProcessorConfiguration();

    private static final String LAST_EVENT_LINK = "last.event.link";
    private static final String LAST_EVENT_PUBLISHED = "last.event.published";
    private static final String LAST_EVENT_TITLE = "last.event.title";
    public static final String EVENT_CHANGE_DATES = "eventChangeDates";

    private PropertiesConfiguration lastEventConfiguration;
    private DB db;
    private BTreeMap<String, Long> eventChangeDatesCollection;

    private EventProcessorConfiguration() {
        initialize();
    }

    private void initialize() {
        try {
            File dataDir = new File(ConfigurationFactory.get().getString("APP_DATA_DIR", "."));
            lastEventConfiguration = new PropertiesConfiguration(new File(dataDir, "lastEvent.properties"));
            db = DBMaker.newFileDB(new File(dataDir, "app_db")).closeOnJvmShutdown().make();
            eventChangeDatesCollection = getEventChangeDatesCollection();
        } catch (ConfigurationException e) {
            throw new IllegalStateException("Failed to access properties");
        }
    }

    public static EventProcessorConfiguration instance() {
        return instance;
    }

    public ProcessEvent loadLastProcessedEvent() {
        String lastEventLink = lastEventConfiguration.getString(LAST_EVENT_LINK, null);
        String lastEventTitle = lastEventConfiguration.getString(LAST_EVENT_TITLE, null);
        long published= lastEventConfiguration.getLong(LAST_EVENT_PUBLISHED, 0);
        if (lastEventLink != null && published > 0) {
            return new ProcessEvent.Builder().title(lastEventTitle).link(lastEventLink).published(new Date(published)).build();
        }
        return null;
    }

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
        lastEventConfiguration.setProperty(LAST_EVENT_LINK, processEvent.getLink());
        lastEventConfiguration.setProperty(LAST_EVENT_PUBLISHED, processEvent.getPublishDate().getTime());
        lastEventConfiguration.setProperty(LAST_EVENT_TITLE, processEvent.getTitle());
        lastEventConfiguration.save();
    }

    public void clearLastProcessEvent() throws ConfigurationException {
        lastEventConfiguration.clearProperty(LAST_EVENT_LINK);
        lastEventConfiguration.clearProperty(LAST_EVENT_PUBLISHED);
        lastEventConfiguration.clearProperty(LAST_EVENT_TITLE);
        lastEventConfiguration.save();
    }

    public void dispose() {
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
