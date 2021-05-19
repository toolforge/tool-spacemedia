package org.wikimedia.commons.donvip.spacemedia.commons.api.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UploadApiResponse {

    private UploadResponse upload;

    private UploadError error;

    @JsonProperty("servedby")
    private String servedBy;

    public UploadResponse getUpload() {
        return upload;
    }

    public void setUpload(UploadResponse upload) {
        this.upload = upload;
    }

    public UploadError getError() {
        return error;
    }

    public void setError(UploadError error) {
        this.error = error;
    }

    public String getServedBy() {
        return servedBy;
    }

    public void setServedBy(String servedBy) {
        this.servedBy = servedBy;
    }

    @Override
    public String toString() {
        return "UploadApiResponse [" + (upload != null ? "upload=" + upload + ", " : "")
                + (error != null ? "error=" + error + ", " : "") + (servedBy != null ? "servedBy=" + servedBy : "")
                + "]";
    }
}
