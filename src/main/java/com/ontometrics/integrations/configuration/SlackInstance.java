package com.ontometrics.integrations.configuration;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ontometrics.integrations.events.*;
import com.ontometrics.integrations.sources.ChannelMapper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by Rob on 8/23/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public class SlackInstance implements ChatServer {

    private Logger log = getLogger(SlackInstance.class);

    private static final String USERNAME_KEY = "username";
    private static final String ICON_URL_KEY = "icon_url";
    private static final String USERNAME = "YouTrack";
    public static final String DEFAULT_ICON_URL = "https://www.jetbrains.com/youtrack/tools/img/youtrack.png";
    public static final String BASE_URL = "https://hooks.slack.com";
    public static final String TEXT_KEY = "text";
    public static final String CHANNEL_KEY = "channel";

    private final ChannelMapper channelMapper;
    private final String iconUrl;

    public SlackInstance(Builder builder) {
        channelMapper = builder.channelMapper;
        iconUrl = builder.icon;
    }

    public static class Builder {

        private ChannelMapper channelMapper;
        private String icon;
        public Builder channelMapper(ChannelMapper channelMapper){
            this.channelMapper = channelMapper;
            return this;
        }

        public SlackInstance build(){
            return new SlackInstance(this);
        }

        public Builder icon(String url) {
            this.icon = url;
            return this;
        }
    }

    @Override
    public void postIssueCreation(Issue issue) {
        postToChannel(channelMapper.getChannel(issue), buildNewIssueMessage(issue));
    }

    @Override
    public void post(IssueEditSession issueEditSession){
        String channel = channelMapper.getChannel(issueEditSession.getIssue());
        postToChannel(channel, buildSessionMessage(issueEditSession));
        
    }

    @Override
    public ChannelMapper getChannelMapper() {
        return channelMapper;
    }

    private void postToChannel(String channel, String message) {
        log.info("posting message {} to channel: {}.", message, channel);
        Client client = ClientBuilder.newClient();

        ObjectNode objectNode = JsonNodeFactory.instance.objectNode().put(USERNAME_KEY, USERNAME)
                .put(ICON_URL_KEY, iconUrl).put(CHANNEL_KEY, channel)
                .put(TEXT_KEY, processMessage(message));

        WebTarget slackApi = client.target(BASE_URL).path(ConfigurationFactory.get().getString("PROP.SLACK_WEBHOOK_PATH"));
        Invocation.Builder invocationBuilder = slackApi.request(MediaType.APPLICATION_JSON_TYPE);
        Response response = invocationBuilder.post(Entity.json(objectNode.toString()));

        log.info("response code: {} response: {}", response.getStatus(), response.readEntity(String.class));
    }

    private String processMessage(String message) {
        return StringUtils.replaceChars(message, "{}", "[]");
    }

    protected String buildSessionMessage(IssueEditSession session) {
        StringBuilder s = new StringBuilder(String.format("*%s*", session.getUpdater()));
        String action = session.getComment() != null && !session.getComment().isDeleted() ? "commented on " : "updated";
        s.append(String.format(" %s %s: ", action, MessageFormatter.getIssueLink(session.getIssue())));
        if (session.getIssue().getTitle()!=null) {
            s.append(session.getIssue().getTitle());
        } else {
            log.debug("title null on issue: {}", session.getIssue());
        }
        s.append(System.lineSeparator());
        for (IssueEdit edit : session.getChanges()){
            s.append(edit.toString()).append(System.lineSeparator());
        }
        if (session.getComment() !=null && !session.getComment().isDeleted()) {
            s.append(session.getComment().getText()).append(System.lineSeparator());
        }
        for (AttachmentEvent attachment : session.getAttachments()){
            s.append("attached ").append(MessageFormatter.getNamedLink(attachment.getFileUrl(), attachment.getName()))
                    .append(System.lineSeparator());
        }

        return s.toString();
    }

    public String buildNewIssueMessage(Issue newIssue){
        return String.format("*%s* created %s: %s%s%s", newIssue.getCreator(), MessageFormatter.getIssueLink(newIssue), newIssue.getTitle(), System.lineSeparator(), newIssue.getDescription());
    }

    private static class MessageFormatter {
        static String getIssueLink(Issue issue){
            return String.format("<%s|%s-%d>", issue.getLink(), issue.getPrefix(), issue.getId());
        }

        static String getNamedLink(String url, String text){
            return String.format("<%s|%s>", url, text);
        }
    }

}

