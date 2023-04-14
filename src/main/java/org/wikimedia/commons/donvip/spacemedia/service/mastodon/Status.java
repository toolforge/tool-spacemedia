package org.wikimedia.commons.donvip.spacemedia.service.mastodon;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a status posted by an account.
 * https://docs.joinmastodon.org/entities/Status/
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Status {

    private String id;

    @JsonProperty("created_at")
    private ZonedDateTime createdAt;

    private String content;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
