package com.ontometrics.integrations.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * Represents a single User (updater) making one or more changes to
 * an {@link com.ontometrics.integrations.events.Issue} at a given
 * time.
 * </p>
 *
 * User: Rob
 * Date: 8/23/14
 * Time: 7:25 PM
 * <p/>
 * (c) ontometrics 2014, All Rights Reserved
 */
public class IssueEditSession {

    private final Issue issue;
    private final String updater;
    private final List<IssueEdit> changes;
    private final List<Comment> comments;
    private final Date updated;
    private final List<AttachmentEvent> attachments;

    public IssueEditSession(Builder builder) {
        issue = builder.issue;
        updater = builder.updater;
        updated = builder.updated;
        changes = new ArrayList<>(builder.changes);
        comments = new ArrayList<>(builder.comments);
        attachments = new ArrayList<>(builder.attachments);
    }

    public static class Builder {

        private Issue issue;
        private String updater;
        private Date updated;
        private List<IssueEdit> changes = Collections.emptyList();
        private List<Comment> comments = Collections.emptyList();
        private List<AttachmentEvent> attachments = Collections.emptyList();

        public Builder issue(Issue issue){
            this.issue = issue;
            return this;
            }

        public Builder updater(String updater){
            this.updater = updater;
            return this;
            }

        public Builder updated(Date updated){
            this.updated = updated;
            return this;
            }

        public Builder changes(List<IssueEdit> changes){
            this.changes = changes;
            return this;
            }

        public Builder comments(List<Comment> comments){
            this.comments = comments;
            return this;
        }

        public Builder attachments(List<AttachmentEvent> attachments){
            this.attachments = attachments;
            return this;
            }

        public Builder attachment(AttachmentEvent attachmentEvent) {
            if (this.attachments.isEmpty()){
                this.attachments = new ArrayList<>();
            }
            this.attachments.add(attachmentEvent);
            return this;
        }

        public IssueEditSession build(){
            return new IssueEditSession(this);
        }


    }

    public Issue getIssue() {
        return issue;
    }

    public String getUpdater() {
        return updater;
    }

    public Date getUpdated() {
        return updated;
    }

    /**
     * The changes that were made in this session.
     *
     * @return all edits made by the updater in this session
     */
    public List<IssueEdit> getChanges() {
        return changes;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public List<AttachmentEvent> getAttachments() {
        return attachments;
    }

    public boolean isCreationEdit(){
        return getIssue().getCreated()!=null && ((getUpdated().getTime()-getIssue().getCreated().getTime())/(1000*60*60) < 5);
    }

    public boolean hasChanges(){
        return isCreationEdit() || getChanges().size() > 0 || getComments().size() > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IssueEditSession that = (IssueEditSession) o;

        return issue.equals(that.issue) && updated.equals(that.updated) && !(updater != null ? !updater.equals(that.updater) : that.updater != null);

    }

    @Override
    public int hashCode() {
        int result = issue.hashCode();
        result = 31 * result + (updater != null ? updater.hashCode() : 0);
        result = 31 * result + updated.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(issue.toString());
        b.append(String.format(" %s ", updater));
        int changeCounter = 0;
        for (IssueEdit edit : changes){
            b.append(edit.toString());
            if (changeCounter++ < changes.size()-1){
                b.append(", ");
            }
        }
        for (Comment comment : comments){
            b.append(comment.toString());
        }
        for (AttachmentEvent attachmentEvent : attachments){
            b.append(attachmentEvent.toString());
        }
        return b.toString();
    }

}
