package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

public class UploadApiResponse extends ApiResponse {

    private UploadResponse upload;

    public UploadResponse getUpload() {
        return upload;
    }

    public void setUpload(UploadResponse upload) {
        this.upload = upload;
    }

    @Override
    public String toString() {
        return "UploadApiResponse [" + (upload != null ? "upload=" + upload + ", " : "")
                + (getError() != null ? "error=" + getError() + ", " : "")
                + (getServedBy() != null ? "servedBy=" + getServedBy() : "")
                + "]";
    }
}
