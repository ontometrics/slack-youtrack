package com.ontometrics.integrations.jobs;

import com.ontometrics.integrations.sources.ChannelMapper;

import java.util.List;
import java.util.Set;

/**
 * Provider of the list of projects (cached)
 */
public class ProjectProvider {
    private static final ProjectProvider instance = new ProjectProvider();

    //temporary here, we'll need to read projects from REST API
    private ChannelMapper channelMapper;

    public static ProjectProvider instance() {
        return instance;
    }

    public ProjectProvider channelMapper(ChannelMapper channelMapper) {
        this.channelMapper = channelMapper;
        return this;
    }

    private ProjectProvider() {}

    /**
     * @return set of all project IDs
     */
    public Set<String> all() {
        return channelMapper.getMappedProjectKeys();
    }

}
