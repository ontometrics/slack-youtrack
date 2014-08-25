package com.ontometrics.integrations.jobs;

import com.google.common.base.Function;
import com.google.common.collect.Multimaps;
import com.ontometrics.integrations.configuration.ChatServer;
import com.ontometrics.integrations.configuration.ConfigurationFactory;
import com.ontometrics.integrations.configuration.EventProcessorConfiguration;
import com.ontometrics.integrations.configuration.YouTrackInstance;
import com.ontometrics.integrations.events.IssueEditSession;
import com.ontometrics.integrations.events.ProcessEvent;
import com.ontometrics.integrations.events.ProcessEventChange;
import com.ontometrics.integrations.sources.EditSessionsExtractor;
import com.ontometrics.integrations.sources.StreamProvider;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
        if(feedStreamProvider == null || chatServer == null) {
            throw new IllegalArgumentException("You must provide sourceURL and chatServer.");
        }


        Configuration configuration = ConfigurationFactory.get();
        editSessionsExtractor = new EditSessionsExtractor(new YouTrackInstance.Builder().baseUrl(
                configuration.getString("YOUTRACK_HOST", YT_FEED_URL))
                .port(configuration.getInt("YOUTRACK_PORT", YT_PORT)).build(), feedStreamProvider);
        editSessionsExtractor.setLastEvent(EventProcessorConfiguration.instance().loadLastProcessedEvent());
        this.chatServer = chatServer;
    }

    /**
     * Load the list of latest-events (list of updated issues since last time this was called.
     * Note: items in this list guarantee that there is no items with same ISSUE-ID exists, they are ordered from most oldest one (first item) to most recent one (last item)
     *
     * For each issue (@link ProcessEvent}) in this list the list of issue change events {@link ProcessEventChange}
     * is loaded. This list will only include the list of updates which were created after {@link #getLastEventChangeDate(com.ontometrics.integrations.events.ProcessEvent)} or after
     * {@link com.ontometrics.integrations.configuration.EventProcessorConfiguration#getDeploymentTime()} in case if there is
     * no last change date for this event.
     *
     * The list of updates for each issue will be grouped by {@link com.ontometrics.integrations.events.ProcessEventChange#getUpdater()}
     * and for each group (group of updates by user) app will generate and post message to external system (slack).
     *
     * @return number of processed events.
     * @throws IOException
     */
    @Override
    public int checkForNewEvents() throws Exception {
        //get events
        List<ProcessEvent> events = editSessionsExtractor.getLatestEvents();

        final AtomicInteger processedSessionsCount = new AtomicInteger(0);
        for (ProcessEvent event : events) {
            processedSessionsCount.incrementAndGet();
            //load list of updates (using REST service of YouTrack)
            List<IssueEditSession> editSessions;
            Date minChangeDate = getLastEventChangeDate(event);
            if (minChangeDate == null) {
                // if there is no last event change date, then minimum issue change date to be retrieved should be
                // after deployment date
                minChangeDate = EventProcessorConfiguration.instance().getDeploymentTime();
            }
            try {
                editSessions = editSessionsExtractor.getEdits(event, minChangeDate);
                postEditSessionsToChatServer(event, editSessions);
            } catch (Exception | Error ex) {
                log.error("Failed to process event " + event, ex);
            } finally {
                //whatever happens, update the last event as processed
                editSessionsExtractor.setLastEvent(event);
                try {
                    EventProcessorConfiguration.instance().saveLastProcessEvent(event);
                } catch (ConfigurationException e) {
                    log.error("Failed to update last processed event", e);
                }
            }
        }

        return processedSessionsCount.get();
    }

    /**
     * Group the list of events by {@link com.ontometrics.integrations.events.ProcessEventChange#getUpdater()}
     * For each group app will generate and post message to chat server.
     *
     * @param event updated issue
     * @param editSessions list of edit sessions for particular issue
     */
    private void postEditSessionsToChatServer(ProcessEvent event, List<IssueEditSession> editSessions) {
        if (editSessions.isEmpty()) {
            log.info("List of edit sessions for event is empty {}");
            chatServer.postIssueCreation(event.getIssue());
        } else {
            try {
                for (Map.Entry<String, Collection<IssueEditSession>> mapEntry : groupChangesByUpdater(editSessions).entrySet()) {
                    for (IssueEditSession editSession : mapEntry.getValue()) {
                        chatServer.post(editSession);
                    }
                }
            } catch (Exception ex) {
                log.error("Failed to post edit sessions to chat server", ex);
            } finally {
                //whatever happens, update the last change date
                EventProcessorConfiguration.instance()
                        .saveEventChangeDate(event, editSessions.get(editSessions.size() - 1).getUpdated());
            }
        }
    }


    private Map<String, Collection<IssueEditSession>> groupChangesByUpdater(List<IssueEditSession> editSessions) {
        return Multimaps.index(editSessions, new Function<IssueEditSession, String>() {
            @Override
            public String apply(IssueEditSession issueEditSession) {
                return issueEditSession.getUpdater();
            }
        }).asMap();
    }

    private Date getLastEventChangeDate(ProcessEvent event) {
        return EventProcessorConfiguration.instance().getEventChangeDate(event);
    }

    public EditSessionsExtractor getEditSessionsExtractor() {
        return editSessionsExtractor;
    }
}
