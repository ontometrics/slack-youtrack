package com.ontometrics.integrations.events;

/**
 * Created by Rob on 8/24/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public class IssueEdit {

    private final Issue issue;
    private final String field;
    private final String priorValue;
    private final String currentValue;

    public IssueEdit(Builder builder) {
        issue = builder.issue;
        field = builder.field;
        priorValue = builder.priorValue;
        currentValue = builder.currentValue;
    }

    public static class Builder {

        private Issue issue;
        private String field;
        private String priorValue;
        private String currentValue;

        public Builder issue(Issue issue){
            this.issue = issue;
            return this;
            }

        public Builder field(String field){
            this.field = field;
            return this;
            }

        public Builder priorValue(String priorValue){
            this.priorValue = priorValue;
            return this;
            }

        public Builder currentValue(String currentValue){
            this.currentValue = currentValue;
            return this;
            }

        public IssueEdit build(){
            return new IssueEdit(this);
            }
    }

    public Issue getIssue() {
        return issue;
    }

    public String getField() {
        return field;
    }

    public String getPriorValue() {
        return priorValue;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(getField());
        if (getPriorValue().length() > 0){
            b.append(getPriorValue()).append(" -> ");
        } else {
            b.append(" set to ");
        }
        b.append(getCurrentValue());
        return b.toString();
    }
}
