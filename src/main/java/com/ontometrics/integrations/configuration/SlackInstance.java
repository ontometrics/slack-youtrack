package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.ProcessEventChange;
import com.ontometrics.integrations.sources.ChannelMapper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by Rob on 8/23/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public class SlackInstance implements ChatServer {

    private Logger log = getLogger(SlackInstance.class);
    public static final String BASE_URL = "https://slack.com";
    public static final String API_PATH = "api";
    public static final String CHANNEL_POST_PATH = "chat.postMessage";
    public static final String TOKEN_KEY = "token";
    public static final String TEXT_KEY = "text";
    public static final String CHANNEL_KEY = "channel";

    private final ChannelMapper channelMapper;

    public SlackInstance(Builder builder) {
        channelMapper = builder.channelMapper;
    }

    public static class Builder {

        private ChannelMapper channelMapper;

        public Builder channelMapper(ChannelMapper channelMapper){
            this.channelMapper = channelMapper;
            return this;
            }

        public SlackInstance build(){
            return new SlackInstance(this);
            }
    }

    @Override
    public void post(ProcessEventChange change){
        String channel = channelMapper.getChannel(change.getIssue());
        postToChannel(channel, buildChangeMessage(change));
        
    }

    private void postToChannel(String channel, String message) {
        log.info("posting message {} to channel: {}.", message, channel);
        Client client = ClientBuilder.newClient();

        WebTarget slackApi = client.target(BASE_URL).path(String.format("%s/%s", API_PATH, CHANNEL_POST_PATH))
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

    protected String buildChangeMessage(ProcessEventChange change) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("*%s*", change.getUpdater()))
                .append(" updated ")
                .append(MessageFormatter.getIssueLink(change.getIssue()))
                .append("\n");
        stringBuilder.append(change.getField()).append(": ");
        if (change.getPriorValue().length() > 0) {
            stringBuilder.append(change.getPriorValue()).append(" -> ");
        }
        stringBuilder.append(change.getCurrentValue());
        return stringBuilder.toString();
    }

    @Override
    public List<String> getUsers(){
        return null;
    }

    private static class MessageFormatter {
        static String getIssueLink(Issue issue){
            return String.format("<%s|%s-%d>", issue.getLink(), issue.getPrefix(), issue.getId());
        }
    }

}

