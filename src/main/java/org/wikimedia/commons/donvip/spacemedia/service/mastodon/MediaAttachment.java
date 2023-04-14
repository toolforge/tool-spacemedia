package org.wikimedia.commons.donvip.spacemedia.service.mastodon;

import java.net.URL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a file or media attachment that can be added to a status.
 * https://docs.joinmastodon.org/entities/MediaAttachment/
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaAttachment {

    private String id;

    private String type;

    private URL url;

    @JsonProperty("preview_url")
    private URL previewUrl;

    @JsonProperty("remote_url")
    private URL remoteUrl;

    private String description;

    private String blurhash;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public URL getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(URL previewUrl) {
        this.previewUrl = previewUrl;
    }

    public URL getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(URL remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBlurhash() {
        return blurhash;
    }

    public void setBlurhash(String blurhash) {
        this.blurhash = blurhash;
    }
}
