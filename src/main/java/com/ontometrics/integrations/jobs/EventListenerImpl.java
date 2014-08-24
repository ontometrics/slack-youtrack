package com.ontometrics.integrations.jobs;

import com.google.common.base.Function;
import com.google.common.collect.Multimaps;
import com.ontometrics.integrations.configuration.ConfigurationAccessError;
import com.ontometrics.integrations.configuration.ConfigurationFactory;
import com.ontometrics.integrations.configuration.EventProcessorConfiguration;
import com.ontometrics.integrations.configuration.YouTrackInstance;
import com.ontometrics.integrations.events.ProcessEvent;
import com.ontometrics.integrations.events.ProcessEventChange;
import com.ontometrics.integrations.sources.ChannelMapper;
import com.ontometrics.integrations.sources.SourceEventMapper;
import com.ontometrics.integrations.sources.StreamProvider;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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

    public static final String TOKEN_KEY = "token";
    public static final String TEXT_KEY = "text";
    public static final String CHANNEL_KEY = "channel";

    private final ChannelMapper channelMapper;
    private SourceEventMapper sourceEventMapper;
    public static final String SLACK_URL = "https://slack.com/api/";
    public static final String CHANNEL_POST_PATH = "chat.postMessage";

    /**
     * @param feedStreamProvider feed resource provider
     * @param channelMapper channelMapper
     */
    public EventListenerImpl(StreamProvider feedStreamProvider, ChannelMapper channelMapper) {

        this.channelMapper = channelMapper;
        if(feedStreamProvider == null || channelMapper == null) throw new IllegalArgumentException("You must provide sourceURL and channelMapper.");


        Configuration configuration = ConfigurationFactory.get();
        sourceEventMapper = new SourceEventMapper(
                new YouTrackInstance.Builder().baseUrl(
                        configuration.getString("YOUTRACK_HOST", YT_FEED_URL))
                        .port(configuration.getInt("YOUTRACK_PORT", YT_PORT)).build(),
                feedStreamProvider);
        sourceEventMapper.setLastEvent(EventProcessorConfiguration.instance().loadLastProcessedEvent());
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
     * Message for each group (group of updates to the issue by particular YouTrack user) is generated with
     * {@link #buildChangesMessage(String, com.ontometrics.integrations.events.ProcessEvent, java.util.Collection)}
     *
     * @return number of processed events.
     * @throws IOException
     */
    @Override
    public int checkForNewEvents() throws Exception {
        //get events
        List<ProcessEvent> events = sourceEventMapper.getLatestEvents();

        final AtomicInteger processedEventsCount = new AtomicInteger(0);
        for (ProcessEvent event : events) {
            processedEventsCount.incrementAndGet();
            //load list of updates (using REST service of YouTrack)
            List<ProcessEventChange> changes;
            Date minChangeDate = getLastEventChangeDate(event);
            if (minChangeDate == null) {
                // if there is no last event change date, then minimum issue change date to be retrieved should be
                // after deployment date
                minChangeDate = EventProcessorConfiguration.instance().getDeploymentTime();
            }
            try {
                changes = sourceEventMapper.getChanges(event, minChangeDate);
                postEventChangesToStream(event, changes, channelMapper.getChannel(event.getIssue()));
            } catch (ConfigurationAccessError error) {
                log.error("Failed to process event " + event, error);
                throw error;
            } catch (Exception ex) {
                log.error("Failed to process event " + event, ex);
            } finally {
                //whatever happens, update the last event as processed
                sourceEventMapper.setLastEvent(event);
                try {
                    EventProcessorConfiguration.instance().saveLastProcessEvent(event);
                } catch (ConfigurationException e) {
                    log.error("Failed to update last processed event", e);
                }
            }
        }

        return processedEventsCount.get();
    }

    /**
     * Group the list of events by {@link com.ontometrics.integrations.events.ProcessEventChange#getUpdater()}
     * For each group app will generate and post message to external system (Slack).
     * Message for each group (group of updates to the issue by particular YouTrack user) is generated with
     * {@link #buildChangesMessage(String, com.ontometrics.integrations.events.ProcessEvent, java.util.Collection)}
     *
     * @param event updated issue
     * @param changes list of change events for particular issue
     * @param channel slack channel
     */
    private void postEventChangesToStream(ProcessEvent event, List<ProcessEventChange> changes, String channel) {
        if (changes.isEmpty()) {
            log.info("List of changes for event is empty {}");
            postMessageToChannel(event, channel, getIssueLink(event));
        } else {
            log.info("List of changes {} for event {}", changes, event);
            for (Map.Entry<String, Collection<ProcessEventChange>> mapEntry : groupChangesByUpdater(changes).entrySet()) {
                try {
                    String updater = mapEntry.getKey();
                    Collection<ProcessEventChange> processEventChanges = mapEntry.getValue();
                    String message = buildChangesMessage(updater, event, processEventChanges);
                    postMessageToChannel(event, channel, message);
                } catch (Exception ex) {
                    log.error("Failed to post event-change. event = "+event, ex);
                }

            }
            //whatever happens, update the last change date
            EventProcessorConfiguration.instance()
                    .saveEventChangeDate(event, changes.get(changes.size() - 1).getUpdated());
        }
    }


    /**
     * @param updater originator of the change
     * @param processEventChanges list of {@link com.ontometrics.integrations.events.ProcessEventChange}
     * @return message about list of changes from updater
     */
    private String buildChangesMessage(String updater, ProcessEvent event, Collection<ProcessEventChange> processEventChanges) {
        String message = String.format("*%s* updated %s\n", updater, getIssueLink(event));
        int i = 0;
        for (ProcessEventChange change : processEventChanges) {

            //noinspection StatementWithEmptyBody
            if (StringUtils.isNotBlank(change.getField())) {
                message += (change.getField() + ": ");
                if (StringUtils.isNotBlank(change.getPriorValue()) && StringUtils.isNotBlank(change.getCurrentValue())) {
                    message += String.format(" %s -> %s", change.getPriorValue(), change.getCurrentValue());
                } else if (StringUtils.isNotBlank(change.getCurrentValue())) {
                    message += change.getCurrentValue();
                }

                if (processEventChanges.size() - 1 < i) {
                    message +="\n";
                }
            } else {
                //TODO: review possible use cases and make sure we output message relevant to change event
                //it could be the case with "LinkChangeField"
//                message += " ?";
            }
            i++;
        }
        return message;
    }

    private Map<String, Collection<ProcessEventChange>> groupChangesByUpdater(List<ProcessEventChange> changes) {
        return Multimaps.index(changes, new Function<ProcessEventChange, String>() {
            @Override
            public String apply(ProcessEventChange processEventChange) {
                return processEventChange.getUpdater();
            }
        }).asMap();
    }

    private Date getLastEventChangeDate(ProcessEvent event) {
        return EventProcessorConfiguration.instance().getEventChangeDate(event);
    }

    /**
     * Post a "message" to a Slack channel identified by "channel"
     * @param event event
     * @param channel name of slack channel
     * @param message text of message to post
     */
    private void postMessageToChannel(ProcessEvent event, String channel, String message){
        log.info("posting event {} message {} .", event.toString(), message);
        Client client = ClientBuilder.newClient();

        WebTarget slackApi = client.target(SLACK_URL).path(CHANNEL_POST_PATH)
                .queryParam(TOKEN_KEY, ConfigurationFactory.get().getString("SLACK_AUTH_TOKEN"))
                .queryParam(TEXT_KEY, processMessage(message))
                .queryParam(CHANNEL_KEY, "#" + channel);

        Invocation.Builder invocationBuilder = slackApi.request(MediaType.APPLICATION_JSON);
        Response response = invocationBuilder.get();

        log.info("response code: {} response: {}", response.getStatus(), response.readEntity(String.class));

    }

    private String processMessage(String message) {
        return StringUtils.replaceChars(message, "{}", "[]");
    }

    private String getIssueLink(ProcessEvent event){
        StringBuilder builder = new StringBuilder();
        String title = event.getTitle();
        title = title.replace(String.valueOf(event.getIssue().toString()), "");
        builder.append("<").append(event.getLink()).append("|").append(event.getIssue().toString()).append(">").append(title);
        return builder.toString();
    }

    public SourceEventMapper getSourceEventMapper() {
        return sourceEventMapper;
    }
}
