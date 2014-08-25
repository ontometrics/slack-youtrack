package com.ontometrics.integrations.sources;

/**
 * Factory for creation of {@link com.ontometrics.integrations.sources.ChannelMapper}
 *
 * ChannelMapperFactory.java
 */
public class ChannelMapperFactory {

    /**
     * @return channel mapper with default projects: ASOC, Job Spider and Dminder
     * //todo use configuration resources to get mapping for projects
     */
    public static ChannelMapper createChannelMapper() {
        return new ChannelMapper.Builder()
                .defaultChannel("process")
                .addMapping("ASOC", "vixlet")
                .addMapping("HA", "jobspider")
                .addMapping("DMAN", "dminder")
                .build();
    }
}
