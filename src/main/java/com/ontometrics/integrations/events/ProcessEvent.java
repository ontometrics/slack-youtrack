package com.ontometrics.integrations.events;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * <p>
 * Represents the record found in the feed that indicates that an
 * Issue was touched.
 * </p>
 * <p>
 * Instances of this class are created when the feed is parsed.
 * </p>
 * <p>
 * Because we have to gather more information about the changes that
 * were made, this class is not used to communicate the substance of
 * what occurred through the chat server.
 * </p>
 *
 * @see com.ontometrics.integrations.events.IssueEditSession
 *
 * User: Rob
 * Date: 7/15/14
 * Time: 8:42 PM
 * <p>
 * (c) ontometrics 2014, All Rights Reserved
 */
public class ProcessEvent {

    private static final String KEY_FIELD_SEPARATOR = "::";

    private final Issue issue;
    private final Date publishDate;

    public ProcessEvent(Builder builder) {
        issue = builder.issue;
        publishDate = builder.publishDate;
    }

    public static class Builder {

        private Date publishDate;
        private Issue issue;

        public Builder issue(Issue issue){
            this.issue = issue;
            return this;
            }

        public Builder published(Date publishDate){
            this.publishDate = publishDate;
            return this;
            }

        public ProcessEvent build(){
            return new ProcessEvent(this);
        }
    }

    public Issue getIssue() {
        return issue;
    }

    public Date getPublishDate() {
        return publishDate;
    }

    /**
     * @return Unique key of the event: combination of issueID and publish Date
     */
    public String getKey() {
        return (issue==null) ? "" : getIssue().getId() + KEY_FIELD_SEPARATOR + createDateFormat().format(publishDate);
    }

    private DateFormat createDateFormat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat;
    }

    @Override
    public String toString() {
        return "ProcessEvent{" +
                " issue=" + getIssue() +
                ", publishDate=" + getPublishDate() +
                '}';
    }
}
