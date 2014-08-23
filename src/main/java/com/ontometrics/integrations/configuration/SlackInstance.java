package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.ProcessEventChange;
import com.ontometrics.integrations.sources.ChannelMapper;

import java.util.List;

/**
 * Created by Rob on 8/23/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public class SlackInstance implements ChatServer {

    public static final String BASE_URL = "https://slack.com/api/";
    public static final String API_PATH = "/api";
    public static final String CHANNEL_POST_PATH = "chat.postMessage";

    private ChannelMapper channelMapper;


    @Override
    public void post(ProcessEventChange change){
        String channel = channelMapper.getChannel(change.getIssue());
    }

    @Override
    public List<String> getUsers(){
        return null;
    }

    private static class MessageFormatter {
        String getIssueLink(Issue issue){
            return String.format("<%s|%s-%d>", issue.getLink(), issue.getPrefix(), issue.getId());
        }
    }

}

