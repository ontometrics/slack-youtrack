package com.ontometrics.integrations.sources;

import com.ontometrics.integrations.events.Issue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by rob on 7/17/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public class ChannelMapper {

    private final Map<String, String> mappings;
    private final String defaultChannel;
    private static final Logger log = LoggerFactory.getLogger(ChannelMapper.class);

    public ChannelMapper(Builder builder) {
        mappings = builder.mappings;
        defaultChannel = builder.defaultChannel;
    }

    public static class Builder {

        Map<String, String> mappings = new HashMap<>();
        private String defaultChannel;

        public Builder defaultChannel(String defaultChannel){
            this.defaultChannel = defaultChannel;
            return this;
        }

        public Builder addMapping(String from, String to){
            mappings.put(from, to);
            return this;
        }

        public ChannelMapper build(){
            return new ChannelMapper(this);
            }
    }


    public String getChannel(final Issue issue){
        String targetChannel = mappings.get(issue.getPrefix());
        log.info("Source: {} Target: {}", issue.getPrefix(), targetChannel);
        return targetChannel != null  ? targetChannel : defaultChannel;
    }


}
