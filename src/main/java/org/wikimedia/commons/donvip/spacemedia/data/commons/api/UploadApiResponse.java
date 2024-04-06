package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadApiResponse extends ApiResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadApiResponse.class);

    private static final Set<String> OK_STATUS = Set.of("Success", "Continue");

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

    public UploadApiResponse checkStatus() throws IOException {
        if (getError() != null || !OK_STATUS.contains(getUpload().getResult())) {
            throw new IOException(toString());
        }
        LOGGER.info("{}", this);
        return this;
    }
}
