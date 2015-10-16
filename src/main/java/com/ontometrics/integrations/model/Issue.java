package com.ontometrics.integrations.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import javax.xml.bind.annotation.XmlAttribute;
import java.util.List;

public class Issue {
    @XmlAttribute
    private String id;

    @JacksonXmlProperty(localName = "field")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<IssueField> fields;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<IssueField> getFields() {
        return fields;
    }

    public void setFields(List<IssueField> fields) {
        this.fields = fields;
    }
}
