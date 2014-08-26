package com.ontometrics.integrations.sources;

import org.apache.commons.configuration.Configuration;

/**
 * Factory for creation of {@link com.ontometrics.integrations.sources.ChannelMapper}
 *
 * ChannelMapperFactory.java
 */
public class ChannelMapperFactory {


    public static final String CHANNEL_MAPPINGS = "channel-mappings";
    public static final String DEFAULT_CHANNEL = "default-channel";

    /**
     * "default-slack-channel" specify list of mappings in the format "${youtrack.project.prefix}->${slack.channel.name}"
     * delimited by ";" (or whatever delimiter which as treated as list delimiter by passed configuration instance"
     * For example "ASOC->vixlet;HA->jobspider;DMAN->dminder"
     * @param configuration configuration
     * @return ChannelMapper instance created from properties "youtrack-to-slack-channels" and "default-slack-channel"
     */
    public static ChannelMapper fromConfiguration(Configuration configuration, String propertyPrefix) {
        String [] mappings = configuration.getStringArray(propertyPrefix + CHANNEL_MAPPINGS);
        String defaultChannel = configuration.getString(propertyPrefix + DEFAULT_CHANNEL);
        ChannelMapper.Builder builder = new ChannelMapper.Builder().defaultChannel(defaultChannel);
        for (String mapping : mappings) {
            String [] keyValue = mapping.split("->");
            if (keyValue.length == 2) {
                builder.addMapping(keyValue[0], keyValue[1]);
            }
        }

        return builder.build();
    }
}
