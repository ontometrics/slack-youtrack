package com.ontometrics.integrations.jobs;

import com.ontometrics.integrations.sources.ChannelMapper;
import com.ontometrics.integrations.sources.InputStreamProvider;
import com.ontometrics.integrations.sources.ProcessEvent;
import com.ontometrics.integrations.sources.SourceEventMapper;
import com.ontometrics.integrations.configuration.ConfigurationFactory;
import com.ontometrics.integrations.sources.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

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
     * TODO rework, so it can authenticate into sourceUrl (we got http-error #401)
     * @param inputStreamProvider url to read list of events from
     * @param channelMapper channelMapper
     */
    public EventListenerImpl(InputStreamProvider inputStreamProvider, ChannelMapper channelMapper) {

        this.channelMapper = channelMapper;
        if(inputStreamProvider == null || channelMapper == null) throw new IllegalArgumentException("You must provide sourceURL and channelMapper.");

        sourceEventMapper = new SourceEventMapper(inputStreamProvider);
    }

    @Override
    public int checkForNewEvents() {
        //get events
        List<ProcessEvent> events = sourceEventMapper.getLatestEvents();

        events.stream().forEach(e -> postEventToChannel(e, channelMapper.getChannel(e)));
        return events.size();
    }

    private void postEventToChannel(ProcessEvent event, String channel){
        log.info("posting event {}.", event.toString());
        Client client = ClientBuilder.newClient();

        WebTarget slackApi = client.target(SLACK_URL).path(CHANNEL_POST_PATH)
                .queryParam(TOKEN_KEY, ConfigurationFactory.get().getString("SLACK_API_KEY"))
                .queryParam(TEXT_KEY, getText(event))
                .queryParam(CHANNEL_KEY, "#" + channel);

        Invocation.Builder invocationBuilder = slackApi.request(MediaType.APPLICATION_JSON);
        Response response = invocationBuilder.get();

        log.info("response code: {} response: {}", response.getStatus(), response.readEntity(String.class));

    }

    private String getText(ProcessEvent event){
        StringBuilder builder = new StringBuilder();
        String title = event.getTitle();
        title = title.replace(event.getID(), "");
        builder.append("<").append(event.getLink()).append("|").append(event.getID()).append(">").append(title);
        return builder.toString();
    }
}
