package com.ontometrics.integrations.events;

/**
 * User: Rob
 * Date: 9/11/14
 * Time: 4:41 PM
 * <p/>
 * (c) ontometrics 2014, All Rights Reserved
 */
public class IssueLink {

    private final String type;
    private final String role;
    private final String relatedIssueID;

    public IssueLink(Builder builder) {
        type = builder.type;
        role = builder.role;
        relatedIssueID = builder.relatedIssueID;
    }

    public static class Builder {

        private String type;
        private String role;
        private String relatedIssueID;

        public Builder type(String type){
            this.type = type;
            return this;
            }

        public Builder role(String role){
            this.role = role;
            return this;
            }

        public Builder relatedIssue(String relatedIssueID){
            this.relatedIssueID = relatedIssueID;
            return this;
            }
        
        public IssueLink build(){
            return new IssueLink(this);
            }
    }

    public String getType() {
        return type;
    }

    public String getRole() {
        return role;
    }

    public String getRelatedIssueID() {
        return relatedIssueID;
    }

    @Override
    public String toString() {
        return "IssueLink{" +
                "type='" + type + '\'' +
                ", role='" + role + '\'' +
                ", relatedIssueID='" + relatedIssueID + '\'' +
                '}';
    }
}
