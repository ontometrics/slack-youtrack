package com.ontometrics.integrations.events;

import java.util.Date;

/**
 * User: Rob
 * Date: 9/1/14
 * Time: 4:43 PM
 * <p/>
 * (c) ontometrics 2014, All Rights Reserved
 */
public class AttachmentEvent {

    private final String name;
    private final Date created;
    private final String author;
    private final String fileUrl;

    public AttachmentEvent(Builder builder) {
        name = builder.name;
        created = builder.created;
        author = builder.author;
        fileUrl = builder.fileUrl;
    }

    public static class Builder {

        private Date created;
        private String fileUrl;
        private String name;
        private String author;

        public Builder name(String name){
            this.name = name;
            return this;
            }

        public Builder created(Date created){
            this.created = created;
            return this;
            }

        public Builder url(String fileUrl){
            this.fileUrl = fileUrl;
            return this;
            }

        public Builder author(String author){
            this.author = author;
            return this;
            }

        public AttachmentEvent build(){
            return new AttachmentEvent(this);
            }
    }

    public String getName() {
        return name;
    }

    public Date getCreated() {
        return created;
    }

    public String getAuthor() {
        return author;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    @Override
    public String toString() {
        return "AttachmentEvent{" +
                "name='" + name + '\'' +
                ", created=" + created +
                ", author='" + author + '\'' +
                ", fileUrl='" + fileUrl + '\'' +
                '}';
    }
}
