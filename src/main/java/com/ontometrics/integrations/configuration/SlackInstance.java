package com.ontometrics.integrations.configuration;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.ontometrics.integrations.events.*;
import com.ontometrics.integrations.sources.ChannelMapper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import javax.ws.rs.client.*;
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

    private static final String USERNAME_KEY = "username";
    private static final String ICON_URL_KEY = "icon_url";
    private static final String USERNAME = "YouTrack";
    public static final String DEFAULT_ICON_URL = "https://www.jetbrains.com/youtrack/tools/img/youtrack.png";
    public static final String BASE_URL = "https://hooks.slack.com";
    public static final String TEXT_KEY = "text";
    public static final String CHANNEL_KEY = "channel";

    private final ChannelMapper channelMapper;
    private final String iconUrl;
    private static final String[] IMAGE_EXTENSIONS = new String[]{"jpg", "jpeg", "gif", "png", "bmp"};

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
        String channel = channelMapper.getChannel(issue);
        postMessageToSlack(createSlackMessage(buildNewIssueMessage(issue), channel));
    }

    @Override
    public void post(IssueEditSession issueEditSession){
        String channel = channelMapper.getChannel(issueEditSession.getIssue());
        String message = buildSessionMessage(issueEditSession);
        ObjectNode slackMessage = createSlackMessage(message, channel);
        addImageAttachments(slackMessage, issueEditSession);

        postMessageToSlack(slackMessage);
    }

    private void addImageAttachments(ObjectNode slackMessage, IssueEditSession issueEditSession) {
        List<AttachmentEvent> imageAttachments = ImmutableList.copyOf(Iterables.filter(issueEditSession.getAttachments(),
                new ImageAttachmentPredicate()));
        //TODO add attachment to slackMessage
    }

    private ObjectNode createSlackMessage(String message, String channel) {
        return JsonNodeFactory.instance.objectNode().put(USERNAME_KEY, USERNAME)
                .put(ICON_URL_KEY, iconUrl).put(CHANNEL_KEY, channel)
                .put(TEXT_KEY, processMessage(message));
    }

    private void postMessageToSlack(ObjectNode messageObj) {
        log.debug("Posting message: {}", messageObj);

        Client client = ClientBuilder.newClient();
        WebTarget slackApi = client.target(BASE_URL).path(ConfigurationFactory.get().getString("PROP.SLACK_WEBHOOK_PATH"));
        Invocation.Builder invocationBuilder = slackApi.request(MediaType.APPLICATION_JSON_TYPE);
        Response response = invocationBuilder.post(Entity.json(messageObj.toString()));

        log.debug("response code: {} response: {}", response.getStatus(), response.readEntity(String.class));
    }

    @Override
    public ChannelMapper getChannelMapper() {
        return channelMapper;
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

        List<AttachmentEvent> nonImageAttachments = getNonImageAttachments(session);
        if (!nonImageAttachments.isEmpty()) {
            Iterable<String> attachmentLinks = Iterables.transform(nonImageAttachments, new Function<AttachmentEvent, String>() {
                @Override
                public String apply(AttachmentEvent attachment) {
                    return MessageFormatter.getNamedLink(attachment.getFileUrl(), attachment.getName());
                }
            });
            s.append("attached ").append(StringUtils.join(attachmentLinks.iterator(), ", "));
        }

        return s.toString();
    }

    private ImmutableList<AttachmentEvent> getNonImageAttachments(IssueEditSession session) {
        return ImmutableList.copyOf(Iterables.filter(session.getAttachments(),
                Predicates.not(new ImageAttachmentPredicate())));
    }


    private static class ImageAttachmentPredicate implements Predicate<AttachmentEvent> {
        @Override
        public boolean apply(AttachmentEvent attachmentEvent) {
            //TODO this is quick prototype now, improve implementation (extract file-name from URL)
            String url = attachmentEvent.getFileUrl();
            for (String imageExtension : IMAGE_EXTENSIONS) {
                if (StringUtils.containsIgnoreCase(url, imageExtension)){
                    return true;
                }
            }
            return false;
        }
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

