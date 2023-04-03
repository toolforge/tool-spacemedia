package org.wikimedia.commons.donvip.spacemedia.service.twitter;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UploadResponse {

    @JsonProperty("media_id")
    private long mediaId;

    @JsonProperty("media_id_string")
    private String mediaIdString;

    private int size;

    @JsonProperty("expires_after_secs")
    private int expiresAfterSecs;

    @JsonProperty("processing_info")
    private ProcessingInfo processingInfo;

    public long getMediaId() {
        return mediaId;
    }

    public void setMediaId(long mediaId) {
        this.mediaId = mediaId;
    }

    public String getMediaIdString() {
        return mediaIdString;
    }

    public void setMediaIdString(String mediaIdString) {
        this.mediaIdString = mediaIdString;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getExpiresAfterSecs() {
        return expiresAfterSecs;
    }

    public void setExpiresAfterSecs(int expiresAfterSecs) {
        this.expiresAfterSecs = expiresAfterSecs;
    }

    public ProcessingInfo getProcessingInfo() {
        return processingInfo;
    }

    public void setProcessingInfo(ProcessingInfo processingInfo) {
        this.processingInfo = processingInfo;
    }

    static class ProcessingInfo {
        private String state;

        @JsonProperty("check_after_secs")
        private int checkAfterSecs;

        @JsonProperty("progress_percent")
        private int progressPercent;

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public int getCheckAfterSecs() {
            return checkAfterSecs;
        }

        public void setCheckAfterSecs(int checkAfterSecs) {
            this.checkAfterSecs = checkAfterSecs;
        }

        public int getProgressPercent() {
            return progressPercent;
        }

        public void setProgressPercent(int progressPercent) {
            this.progressPercent = progressPercent;
        }
    }
}
