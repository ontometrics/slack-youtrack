package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.Issue;

import java.net.URL;
import java.util.Date;

/**
 * Created by rob on 8/19/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public interface IssueTracker {

    URL getBaseUrl();

    URL getFeedUrl(String project, Date sinceDate);

    URL getChangesUrl(Issue issue);

    URL getAttachmentsUrl(Issue issue);

    String getIssueRestUrl(Issue issue);

    URL getIssueUrl(String issueIdentifier);
}
