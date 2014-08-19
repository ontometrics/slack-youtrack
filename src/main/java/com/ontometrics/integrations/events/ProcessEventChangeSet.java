package com.ontometrics.integrations.events;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Rob on 7/23/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public class ProcessEventChangeSet {

    private final Date changedOn;
    private final String editor;
    private final Issue issue;
    private final List<ProcessEventChange> changes;

    public ProcessEventChangeSet(Builder builder) {
        changedOn = builder.changedOn;
        editor = builder.editor;
        changes = builder.changes;
        issue = builder.issue;
    }

    public static class Builder {

        private String editor;
        private Date changedOn;
        private List<ProcessEventChange> changes;
        private Issue issue;

        public Builder issue(Issue issue){
            this.issue = issue;
            return this;
            }

        public Builder editor(String editor){
            this.editor = editor;
            return this;
            }

        public Builder changedOn(Date changedOn){
            this.changedOn = changedOn;
            return this;
            }

        public Builder changes(List<ProcessEventChange> changes){
            this.changes = changes;
            return this;
            }

        public Builder change(ProcessEventChange change) {
            changes = new ArrayList<>(1);
            changes.add(change);
            return this;
        }

        public ProcessEventChangeSet build(){
            return new ProcessEventChangeSet(this);
        }

    }

    public Issue getIssue() {
        return issue;
    }

    public Date getChangedOn() {
        return changedOn;
    }

    public String getEditor() {
        return editor;
    }

    public List<ProcessEventChange> getChanges() {
        return changes;
    }

    @Override
    public String toString() {
        return "ProcessEventChangeSet{" +
                "issue=" + issue +
                ", changedOn=" + changedOn +
                ", editor='" + editor + '\'' +
                ", changes=" + changes +
                '}';
    }
}
