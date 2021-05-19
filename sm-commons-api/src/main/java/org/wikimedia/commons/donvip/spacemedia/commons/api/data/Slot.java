package org.wikimedia.commons.donvip.spacemedia.commons.api.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Slot {

    @JsonProperty("contentmodel")
    private String contentModel;

    @JsonProperty("contentformat")
    private String contentFormat;

    @JsonProperty("*")
    private String content;

    public String getContentModel() {
        return contentModel;
    }

    public void setContentModel(String contentModel) {
        this.contentModel = contentModel;
    }

    public String getContentFormat() {
        return contentFormat;
    }

    public void setContentFormat(String contentFormat) {
        this.contentFormat = contentFormat;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Slot [contentModel=" + contentModel + ", contentFormat=" + contentFormat + ", content=" + content + "]";
    }
}
