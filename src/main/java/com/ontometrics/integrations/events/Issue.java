package com.ontometrics.integrations.events;

import java.net.URL;

/**
 * Created by Rob on 8/19/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public class Issue {

    private final int id;
    private final String prefix;
    private final String title;
    private final String description;
    private final URL link;

    public Issue(Builder builder) {
        id = builder.id;
        prefix = builder.prefix;
        title = builder.title;
        description = builder.description;
        link = builder.link;
    }

    public static class Builder {

        private int id;
        private String prefix;
        private String title;
        private String description;
        private URL link;

        public Builder id(int id){
            this.id = id;
            return this;
            }

        public Builder projectPrefix(String prefix){
            this.prefix = prefix;
            return this;
            }

        public Builder title(String title){
            this.title = title;
            return this;
            }

        public Builder description(String description){
            this.description = description;
            return this;
            }

        public Builder link(URL link){
            this.link = link;
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

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public URL getLink() {
        return link;
    }

    @Override
    public String toString() {
        return prefix + "-" + id;
    }
}
