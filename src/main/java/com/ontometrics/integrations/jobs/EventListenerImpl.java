package com.ontometrics.integrations.jobs;

import com.ontometrics.integrations.configuration.ConfigurationFactory;
import com.ontometrics.integrations.configuration.EventProcessorConfiguration;
import com.ontometrics.integrations.events.ProcessEvent;
import com.ontometrics.integrations.events.ProcessEventChange;
import com.ontometrics.integrations.sources.*;
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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created on 8/18/14.
 *
 */
public class EventListenerImpl implements EventListener {
    private static final Logger log = LoggerFactory.getLogger(EventListenerImpl.class);

    public static final String TOKEN_KEY = "token";
    public static final String TEXT_KEY = "text";
    public static final String CHANNEL_KEY = "channel";

    private final ChannelMapper channelMapper;
    private SourceEventMapper sourceEventMapper;
    public static final String SLACK_URL = "https://slack.com/api/";
    public static final String CHANNEL_POST_PATH = "chat.postMessage";

    /**
     * @param inputStreamProvider url to read list of events from
     * @param channelMapper channelMapper
     */
    public EventListenerImpl(InputStreamProvider inputStreamProvider, ChannelMapper channelMapper) {

        this.channelMapper = channelMapper;
        if(inputStreamProvider == null || channelMapper == null) throw new IllegalArgumentException("You must provide sourceURL and channelMapper.");


        sourceEventMapper = new SourceEventMapper(inputStreamProvider);
        sourceEventMapper.setLastEvent(EventProcessorConfiguration.instance().loadLastProcessedEvent());
    }

    /**
     * Load the list of latest-events (list of updated issues since last time the {@link #checkForNewEvents()} was called.
     * Note: items in this list guarantee that there is no items with same ISSUE-ID exists
     * For each issue (@link ProcessEvent}) in this list the list of issue change events {@link ProcessEventChange}
     * is loaded. This list will only include the list of updates which were created after {@link #getLastEventChangeDate(com.ontometrics.integrations.events.ProcessEvent)}
     * The list of updates for each issue will be grouped by {@link com.ontometrics.integrations.events.ProcessEventChange#getUpdater()}
     * and for each group (group of updates by user) app will generate and post message to external system (slack).
     * Message for each group (group of updates to the issue by particular YouTrack user) is generated with {@link #buildChangesMessage(String, com.ontometrics.integrations.events.ProcessEvent, java.util.List)}
     *
     * @return number of processed events.
     * @throws IOException
     */
    @Override
    public int checkForNewEvents() throws IOException {
        //get events
        List<ProcessEvent> events = sourceEventMapper.getLatestEvents();

        final AtomicInteger processedEventsCount = new AtomicInteger(0);
        try {
            events.stream().forEach(e -> {
                processedEventsCount.incrementAndGet();
                //load list of updates (using REST service of YouTrack)
                List<ProcessEventChange> changes = sourceEventMapper.getChanges(e, getLastEventChangeDate(e));

                postEventChangesToStream(e, changes, channelMapper.getChannel(e));
            });
        } catch (Exception ex) {
            log.error("Got error while processing event changes feed", ex);
        }

        return processedEventsCount.get();
    }

    /**
     * Group the lit of events by {@link com.ontometrics.integrations.events.ProcessEventChange#getUpdater()}
     * For each group app will generate and post message to external system (slack).
     * Message for each group (group of updates to the issue by particular YouTrack user) is generated with {@link #buildChangesMessage(String, com.ontometrics.integrations.events.ProcessEvent, java.util.List)}
     *
     * @param event updated issue
     * @param changes list of change events for particular issue
     * @param channel slack channel
     */
    private void postEventChangesToStream(ProcessEvent event, List<ProcessEventChange> changes, String channel) {
        if (changes.isEmpty()) {
            postMessageToChannel(event, channel, getIssueLink(event));
        } else {
            groupChangesByUpdater(changes).forEach((updater, processEventChanges) -> {
                postMessageToChannel(event, channel, buildChangesMessage(updater, event, processEventChanges));
            });
            EventProcessorConfiguration.instance()
                    .saveEventChangeDate(event, changes.get(changes.size() - 1).getUpdated());
        }
        sourceEventMapper.setLastEvent(event);
        try {
            EventProcessorConfiguration.instance().saveLastProcessEvent(event);
        } catch (ConfigurationException e) {
            throw new RuntimeException("Failed to update last processed event", e);
        }
    }

    /**
     * @param updater originator of the change
     * @param processEventChanges list of {@link com.ontometrics.integrations.events.ProcessEventChange}
     * @return message about list of changes from updater
     */
    private String buildChangesMessage(String updater, ProcessEvent event, List<ProcessEventChange> processEventChanges) {
        String message = "*" + updater + "* updated "+getIssueLink(event)+"\n";

        for (int i = 0; i < processEventChanges.size(); i++) {
            ProcessEventChange change = processEventChanges.get(i);
            if (StringUtils.isNotBlank(change.getField())) {
                message += (change.getField() + ": ");
                if (StringUtils.isNotBlank(change.getPriorValue()) && StringUtils.isNotBlank(change.getCurrentValue())) {
                    message += change.getPriorValue() + " -> " + change.getCurrentValue();
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
        }
        return message;
    }

    private Map<String, List<ProcessEventChange>> groupChangesByUpdater(List<ProcessEventChange> changes) {
        return changes.stream().collect(Collectors.groupingBy(ProcessEventChange::getUpdater));
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
                .queryParam(TEXT_KEY, message)
                .queryParam(CHANNEL_KEY, "#" + channel);

        Invocation.Builder invocationBuilder = slackApi.request(MediaType.APPLICATION_JSON);
        Response response = invocationBuilder.get();

        log.info("response code: {} response: {}", response.getStatus(), response.readEntity(String.class));

    }

    private String getIssueLink(ProcessEvent event){
        StringBuilder builder = new StringBuilder();
        String title = event.getTitle();
        title = title.replace(event.getID(), "");
        builder.append("<").append(event.getLink()).append("|").append(event.getID()).append(">").append(title);
        return builder.toString();
    }

    public SourceEventMapper getSourceEventMapper() {
        return sourceEventMapper;
    }
}
