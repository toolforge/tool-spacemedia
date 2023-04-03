package org.wikimedia.commons.donvip.spacemedia.service.twitter;

import com.fasterxml.jackson.annotation.JsonProperty;

class UploadRequest {
    private String command;
    @JsonProperty("media_type")
    private String mediaType;
    @JsonProperty("total_bytes")
    private long totalBytes;
    @JsonProperty("media_category")
    private String mediaCategory;

    public UploadRequest() {
        // Default constructor for jackson
    }

    public UploadRequest(String command, String mediaType, long totalBytes, String mediaCategory) {
        this.command = command;
        this.mediaType = mediaType;
        this.totalBytes = totalBytes;
        this.mediaCategory = mediaCategory;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(int totalBytes) {
        this.totalBytes = totalBytes;
    }

    public String getMediaCategory() {
        return mediaCategory;
    }

    public void setMediaCategory(String mediaCategory) {
        this.mediaCategory = mediaCategory;
    }
}