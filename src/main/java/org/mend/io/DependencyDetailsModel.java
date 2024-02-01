package org.mend.io;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class DependencyDetailsModel implements Serializable {

    private static final long serialVersionUID = 7225050324387373403L;

    @JsonProperty("library")
    private String library;
    @JsonProperty("description")
    private String description;
    @JsonProperty("reference")
    private String reference;
    @JsonProperty("referenceLink")
    private String referenceLink;
    @JsonProperty("filePathList")
    private String[] filePathList;
    @JsonProperty("version")
    private String version;
    @JsonProperty("issueType")
    private String issueType;

    public DependencyDetailsModel() {
    }

    public String getLibrary() {
        return library;
    }

    public void setLibrary(String library) {
        this.library = library;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getReferenceLink() {
        return referenceLink;
    }

    public void setReferenceLink(String referenceLink) {
        this.referenceLink = referenceLink;
    }

    public String[] getFilePathList() {
        return filePathList;
    }

    public void setFilePathList(String[] filePathList) {
        this.filePathList = filePathList;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }
}

