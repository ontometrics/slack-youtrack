package com.ontometrics.integrations.events;

/**
 * Created by Rob on 8/19/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public class Issue {

    private final int id;
    private final String prefix;

    public Issue(Builder builder) {
        id = builder.id;
        prefix = builder.prefix;
    }

    public static class Builder {

        private int id;
        private String prefix;

        public Builder id(int id){
            this.id = id;
            return this;
            }

        public Builder projectPrefix(String prefix){
            this.prefix = prefix;
            return this;
            }

        public Issue build(){
            return new Issue(this);
            }
    }

    public int getId() {
        return id;
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    public String toString() {
        return prefix + "-" + id;
    }
}
