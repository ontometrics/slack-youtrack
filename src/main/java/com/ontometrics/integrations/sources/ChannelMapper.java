package com.ontometrics.integrations.sources;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.ontometrics.integrations.events.ProcessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by rob on 7/17/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public class ChannelMapper {

    private final Map<String, String> mappings;
    private final String defaultChannel;
    private Logger log = LoggerFactory.getLogger(ChannelMapper.class);

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


    public String getChannel(final ProcessEvent event){
        String channelKey = Iterables.find(mappings.keySet(), new Predicate<String>() {
            @Override
            public boolean apply(String k) {
                return event.getTitle().contains(k);
            }
        }, null);

//        Optional<String> channelKey = mappings.keySet().stream().filter(k -> event.getTitle().contains(k)).findFirst();
        log.info("channelKey: {} isPresent: {}", channelKey, channelKey != null);
        return channelKey != null  ? mappings.get(channelKey) : defaultChannel;
    }


}
