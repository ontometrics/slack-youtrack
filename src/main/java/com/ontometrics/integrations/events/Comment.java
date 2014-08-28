package com.ontometrics.integrations.events;

import java.util.Date;

/**
 * Created by rob on 8/28/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public class Comment {

    private final String author;
    private final Date created;
    private final String text;

    public Comment(Builder builder) {
        author = builder.author;
        created = builder.created;
        text = builder.text;
    }

    public static class Builder {

        private Date created;
        private String author;
        private String text;

        public Builder author(String author){
            this.author = author;
            return this;
            }

        public Builder text(String text){
            this.text = text;
            return this;
            }

        public Builder created(Date created){
            this.created = created;
            return this;
            }
        
        public Comment build(){
            return new Comment(this);
            }
    }

    public String getAuthor() {
        return author;
    }

    public Date getCreated() {
        return created;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return String.format("%s on %s: %s%s", author, created, text, System.lineSeparator());
    }
}
