package com.ontometrics.integrations.events;

import java.util.Date;

/**
 * Created by rob on 7/23/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public class ProcessEventChange {

    private final String field;
    private final String priorValue;
    private final String currentValue;
    private final String updater;
    private final Date updated;

    public ProcessEventChange(Builder builder) {
        updater = builder.updater;
        updated = builder.updated;
        field = builder.field;
        priorValue = builder.priorValue;
        currentValue = builder.currentValue;
    }

    public static class Builder {

        private String field;
        private String priorValue;
        private String currentValue;
        private String updater;
        private Date updated;

        public Builder updater(String updater){
            this.updater = updater;
            return this;
            }

        public Builder updated(Date updated){
            this.updated = updated;
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

        public Builder currentValue(String currentVale){
            this.currentValue = currentVale;
            return this;
            }

        public ProcessEventChange build(){
            return new ProcessEventChange(this);
            }
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

    public Date getUpdated() {
        return updated;
    }

    public String getUpdater() {
        return updater;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(String.format("%s changed %s ", updater, field));
        if (priorValue.length() > 0) {
            stringBuilder.append("from " + priorValue + " ");
        }
        stringBuilder.append("to " +  currentValue);
        return stringBuilder.toString();
    }
}
