package com.ontometrics.integrations.events;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * User: Rob
 * Date: 8/23/14
 * Time: 7:25 PM
 * <p/>
 * (c) ontometrics 2014, All Rights Reserved
 */
public class IssueEditSession {

    private final Issue issue;
    private final String updater;
    private final List<ProcessEventChange> changes;
    private final Date updated;

    public IssueEditSession(Builder builder) {
        issue = builder.issue;
        updater = builder.updater;
        updated = builder.updated;
        changes = new ArrayList<>(builder.changes);
    }

    public static class Builder {

        private Issue issue;
        private String updater;
        private List<ProcessEventChange> changes;
        private Date updated;

        public Builder issue(Issue issue){
            this.issue = issue;
            return this;
            }

        public Builder updater(String updater){
            this.updater = updater;
            return this;
            }

        public Builder updated(Date updated){
            this.updated = updated;
            return this;
            }

        public Builder changes(List<ProcessEventChange> changes){
            this.changes = changes;
            return this;
            }

        public IssueEditSession build(){
            return new IssueEditSession(this);
            }
    }

    public Issue getIssue() {
        return issue;
    }

    public String getUpdater() {
        return updater;
    }

    public Date getUpdated() {
        return updated;
    }

    public List<ProcessEventChange> getChanges() {
        return changes;
    }

    @Override
    public String toString() {
        return "IssueEditSession{" +
                "issue=" + issue +
                ", updater='" + updater + '\'' +
                ", changes=" + changes +
                ", updated=" + updated +
                '}';
    }
}
