package com.ontometrics.integrations.events;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * Represents a single User (updater) making one or more changes to
 * an {@link com.ontometrics.integrations.events.Issue} at a given
 * time.
 * </p>
 *
 * User: Rob
 * Date: 8/23/14
 * Time: 7:25 PM
 * <p/>
 * (c) ontometrics 2014, All Rights Reserved
 */
public class IssueEditSession {

    private final Issue issue;
    private final String updater;
    private final List<IssueEdit> changes;
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
        private List<IssueEdit> changes;
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

        public Builder changes(List<IssueEdit> changes){
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

    /**
     * The changes that were made in this session.
     *
     * @return all edits made by the updater in this session
     */
    public List<IssueEdit> getChanges() {
        return changes;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(issue.toString());
        b.append(String.format(" %s ", updater));
        int changeCounter = 0;
        for (IssueEdit edit : changes){
            b.append(edit.toString());
            if (changeCounter++ < changes.size()-1){
                b.append(", ");
            }
        }
        return b.toString();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IssueEditSession that = (IssueEditSession) o;

        if (!issue.equals(that.issue)) return false;
        if (!updated.equals(that.updated)) return false;
        if (updater != null ? !updater.equals(that.updater) : that.updater != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = issue.hashCode();
        result = 31 * result + (updater != null ? updater.hashCode() : 0);
        result = 31 * result + updated.hashCode();
        return result;
    }
}
