package com.ontometrics.integrations.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.util.ArrayList;
import java.util.List;

@JacksonXmlRootElement(localName = "projects")
@XmlSeeAlso({Issue.class, IssueField.class, IssueFieldValue.class})
public class ProjectList {
    @JacksonXmlProperty(localName = "project")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Project> projects = new ArrayList<>();


    public List<Project> getProjects() {
        return projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }

    public static class Project {
        @XmlAttribute
        private String shortName;

        public String getShortName() {
            return shortName;
        }

        public void setShortName(String shortName) {
            this.shortName = shortName;
        }
    }
}
