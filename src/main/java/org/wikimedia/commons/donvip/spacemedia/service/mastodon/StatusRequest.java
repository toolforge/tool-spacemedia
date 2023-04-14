package org.wikimedia.commons.donvip.spacemedia.service.mastodon;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Publish a status with the given parameters.
 * https://docs.joinmastodon.org/methods/statuses/#create
 */
public class StatusRequest {

    private String status;

    @JsonProperty("media_ids")
    private List<String> mediaIds;

    public StatusRequest(String status, List<String> mediaIds) {
        this.status = status;
        this.mediaIds = mediaIds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getMediaIds() {
        return mediaIds;
    }

    public void setMediaIds(List<String> mediaIds) {
        this.mediaIds = mediaIds;
    }
}
