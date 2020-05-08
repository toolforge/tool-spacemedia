package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response to the Hubble/James Webb {@code news} API call. See
 * <a href="http://hubblesite.org/api/documentation#images">documentation</a>.
 */
public class HubbleNasaNewsResponse {

    /**
     * Internal key to identify the release. It can be used to gather more
     * information using the details API call.
     */
    @JsonProperty("news_id")
    private String id;

    /**
     * Title of the News Release
     */
    private String name;

    /**
     * URL of the news release
     */
    private String url;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "HubbleNasaNewsResponse [id=" + id + ", name=" + name + "]";
    }
}
