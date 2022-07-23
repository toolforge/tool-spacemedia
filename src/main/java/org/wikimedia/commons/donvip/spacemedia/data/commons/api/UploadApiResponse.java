package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UploadApiResponse {

    private UploadResponse upload;

    private ApiError error;

    @JsonProperty("servedby")
    private String servedBy;

    public UploadResponse getUpload() {
        return upload;
    }

    public void setUpload(UploadResponse upload) {
        this.upload = upload;
    }

    public ApiError getError() {
        return error;
    }

    public void setError(ApiError error) {
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
