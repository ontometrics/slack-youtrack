package com.ontometrics.integrations.sources;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Rob on 7/23/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public class EditSet {

    private final Date changedOn;
    private final String editor;
    private final List<ProcessEventChange> changes;

    public EditSet(Builder builder) {
        changedOn = builder.changedOn;
        editor = builder.editor;
        changes = builder.changes;
    }

    public static class Builder {

        private String editor;
        private Date changedOn;
        private List<ProcessEventChange> changes;

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

        public EditSet build(){
            return new EditSet(this);
        }

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
        return "EditSet{" +
                "changedOn=" + changedOn +
                ", editor='" + editor + '\'' +
                ", changes=" + changes +
                '}';
    }
}
