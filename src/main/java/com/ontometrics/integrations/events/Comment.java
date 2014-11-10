package com.ontometrics.integrations.events;

import java.util.Date;

/**
 * Created by rob on 8/28/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public class Comment {
    private String id;
    private final String author;
    private final Date created;
    private final String text;
    private boolean deleted;


    public Comment(Builder builder) {
        id = builder.id;
        author = builder.author;
        created = builder.created;
        text = builder.text;
        deleted = builder.deleted;
    }

    public static class Builder {
        private String id;
        private Date created;
        private String author;
        private String text;
        private boolean deleted;


        public Builder id(String id){
            this.id = id;
            return this;
        }

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

        public Builder deleted(boolean deleted) {
            this.deleted = deleted;
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

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Comment comment = (Comment) o;

        return id.equals(comment.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s on %s: %s%s", author, created, text, System.lineSeparator());
    }
}
