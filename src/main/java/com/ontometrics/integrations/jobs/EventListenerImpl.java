package com.ontometrics.integrations.jobs;

import com.ontometrics.integrations.configuration.ChatServer;
import com.ontometrics.integrations.configuration.ConfigurationFactory;
import com.ontometrics.integrations.configuration.EventProcessorConfiguration;
import com.ontometrics.integrations.configuration.YouTrackInstance;
import com.ontometrics.integrations.events.IssueEditSession;
import com.ontometrics.integrations.sources.EditSessionsExtractor;
import com.ontometrics.integrations.sources.StreamProvider;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created on 8/18/14.
 *
 */
public class EventListenerImpl implements EventListener {
    private static final Logger log = LoggerFactory.getLogger(EventListenerImpl.class);

    private static final String YT_FEED_URL = "http://ontometrics.com";
    private static final int YT_PORT = 8085;
    private ChatServer chatServer;

    private EditSessionsExtractor editSessionsExtractor;

    /**
     * @param feedStreamProvider feed resource provider
     * @param chatServer chat server
     */
    public EventListenerImpl(StreamProvider feedStreamProvider, ChatServer chatServer) {
        this(createEditSessionExtractor(feedStreamProvider), chatServer);
    }

    private static EditSessionsExtractor createEditSessionExtractor(StreamProvider feedStreamProvider) {
        if(feedStreamProvider == null) {
            throw new IllegalArgumentException("You must provide feedStreamProvider.");
        }
        Configuration configuration = ConfigurationFactory.get();
        return new EditSessionsExtractor(new YouTrackInstance.Builder().baseUrl(
                configuration.getString("PROP.YOUTRACK_HOST", YT_FEED_URL))
                .port(configuration.getInt("PROP.YOUTRACK_PORT", YT_PORT)).build(), feedStreamProvider);
    }

    /**
     * @param editSessionsExtractor editSessionsExtractor
     * @param chatServer chat server
     */
    public EventListenerImpl(EditSessionsExtractor editSessionsExtractor, ChatServer chatServer) {
        if(editSessionsExtractor == null || chatServer == null) {
            throw new IllegalArgumentException("You must provide sourceURL and chatServer.");
        }
        this.chatServer = chatServer;
        this.editSessionsExtractor = editSessionsExtractor;
    }

    /**
     * <p>
     * On wake, the job of this agent is simply to get any edits that have occurred since its last run from
     * the ticketing system (using the {@link com.ontometrics.integrations.sources.EditSessionsExtractor} and
     * then post them to the {@link com.ontometrics.integrations.configuration.ChatServer}.
     * </p>
     * <p>
     * This should stay simple: if we can't process a session for any reason we should skip it.
     * </p>
     *
     * @return the number of sessions that were processed
     * @throws Exception if it fails to save the last event date
     */
    @Override
    public int checkForNewEvents() throws Exception {
        //get events
        EventProcessorConfiguration eventProcessorConfiguration = EventProcessorConfiguration.instance();
        Date minDateOfEvents = eventProcessorConfiguration
                .resolveMinimumAllowedDate(eventProcessorConfiguration.loadLastProcessedDate());

        List<IssueEditSession> editSessions = editSessionsExtractor.getLatestEdits(minDateOfEvents);

        log.info("Found {} edit sessions to post.", editSessions.size());
        final AtomicInteger processedSessionsCount = new AtomicInteger(0);
        if (editSessions.size() > 0) {
            log.debug("sessions: {}", editSessions);
            Date lastProcessedSessionDate = null;
            for (IssueEditSession session : editSessions) {
                if (session.isCreationEdit()) {
                    chatServer.postIssueCreation(session.getIssue());
                } else {
                    chatServer.post(session);
                }
                lastProcessedSessionDate = session.getUpdated();
                processedSessionsCount.incrementAndGet();
            }

            log.debug("setting last processed date to: {}", lastProcessedSessionDate);
            EventProcessorConfiguration.instance().saveLastProcessedEventDate(lastProcessedSessionDate);
        }
        return processedSessionsCount.get();
    }


}
