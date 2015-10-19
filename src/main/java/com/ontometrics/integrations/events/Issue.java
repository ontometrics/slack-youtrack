package com.ontometrics.integrations.events;

import java.net.URL;
import java.util.Date;

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
    private final String creator;
    private final Date created;
    private final URL externalLink;

    public Issue(Builder builder) {
        id = builder.id;
        prefix = builder.prefix;
        creator = builder.creator;
        created = builder.created;
        title = builder.title;
        description = builder.description;
        link = builder.link;
        this.externalLink = builder.externalLink;
    }

    public static class Builder {

        private int id;
        private String prefix;
        private String title;
        private String description;
        private URL link;
        private URL externalLink;
        private String creator;
        private Date created;

        public Builder id(int id){
            this.id = id;
            return this;
            }

        public Builder projectPrefix(String prefix){
            this.prefix = prefix;
            return this;
            }

        public Builder creator(String creator){
            this.creator = creator;
            return this;
            }

        public Builder created(Date created){
            this.created = created;
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

        public Builder externalLink(URL link){
            this.externalLink = link;
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

    public String getCreator() {
        return creator;
    }

    public Date getCreated() {
        return created;
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

    public URL getExternalLink() {
        return externalLink;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Issue issue = (Issue) o;

        return id == issue.id && !(prefix != null ? !prefix.equals(issue.prefix) : issue.prefix != null);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Issue{" +
                "id=" + id +
                ", prefix='" + prefix + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", link=" + link +
                ", creator='" + creator + '\'' +
                ", created=" + created +
                '}';
    }
}
