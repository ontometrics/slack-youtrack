package com.ontometrics.integrations.jobs;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ontometrics.integrations.configuration.IssueTracker;
import com.ontometrics.integrations.model.IssueList;
import com.ontometrics.integrations.model.ProjectList;
import com.ontometrics.integrations.sources.ChannelMapper;
import com.ontometrics.integrations.sources.InputStreamHandler;
import com.ontometrics.integrations.sources.StreamProvider;
import com.ontometrics.util.BadResponseException;
import com.ontometrics.util.HttpUtil;
import com.ontometrics.util.Mapper;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.http.HttpStatus;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * Provider of the list of projects (cached)
 */
public class ProjectProvider {

    /**
     * 1 hour
     */
    private static final long PROJECT_REFRESH_INTERVAL = 3600*1000;

    private Set<String> projects;
    private IssueTracker issueTracker;
    private StreamProvider streamProvider;

    private Long nextUpdateTime;

    public ProjectProvider(IssueTracker issueTracker, StreamProvider streamProvider) {
        this.issueTracker = issueTracker;
        this.streamProvider = streamProvider;
    }

    /**
     * @return set of all project IDs
     */
    public Set<String> all() throws Exception {
        if (projects == null || (nextUpdateTime != null && System.currentTimeMillis() > nextUpdateTime)) {
            projects = loadProjectList();
            nextUpdateTime = System.currentTimeMillis() + PROJECT_REFRESH_INTERVAL;
        }
        return projects;
    }

    public Set<String> loadProjectList() throws Exception {
        final URL url = new URL(String.format("%s/rest/project/all", issueTracker.getBaseUrl()));
        return Sets.newHashSet(streamProvider.openResourceStream(url, new InputStreamHandler<List<String>>() {
            @Override
            public List<String> handleStream(InputStream is, int responseCode) throws Exception {
                HttpUtil.checkResponseCode(responseCode, url);
                ProjectList projectList = Mapper.createXmlMapper().readValue(is, ProjectList.class);
                return Lists.transform(projectList.getProjects(), new Function<ProjectList.Project, String>() {
                    @Override
                    public String apply(ProjectList.Project project) {
                        return project.getShortName();
                    }
                });
            }
        }));
    }


}
