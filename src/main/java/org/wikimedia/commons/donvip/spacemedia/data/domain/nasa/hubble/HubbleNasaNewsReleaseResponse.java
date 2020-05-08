package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble;

import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response to the Hubble/James Webb {@code news_release} API call. See
 * <a href="http://hubblesite.org/api/documentation#images">documentation</a>.
 */
public class HubbleNasaNewsReleaseResponse {

    /**
     * Internal key to identify the release. It can be used to gather more
     * information using the details API call.
     */
    @JsonProperty("news_id")
    private String id;

    /**
     * Title of the News Release.
     */
    private String name;

    /**
     * URL of the news release.
     */
    private String url;

    /**
     * Publication date and time.
     */
    private ZonedDateTime publication;

    /**
     * Mission.
     */
    private String mission;

    /**
     * Story's abstract text (HTML in some cases).
     */
    @JsonProperty("abstract")
    private String _abstract;

    /**
     * Story's credits and acknowledgments (usually includes HTML tags).
     */
    private String credits;

    /**
     * HTTPS URL of a thumbnail image, 200x200 pixels.
     */
    private String thumbnail;

    /**
     * HTTPS URL of a thumbnail image, 400x400 pixels.
     */
    @JsonProperty("thumbnail_retina")
    private String thumbnailRetina;

    /**
     * HTTPS URL of a thumbnail image, 200x200 pixels.
     */
    @JsonProperty("thumbnail_1x")
    private String thumbnail1x;

    /**
     * HTTPS URL of a thumbnail image, 400x400 pixels.
     */
    @JsonProperty("thumbnail_2x")
    private String thumbnail2x;

    /**
     * HTTPS URL of a Keystone image, 678x260 pixels.
     */
    @JsonProperty("keystone_image_1x")
    private String keystoneImage1x;

    /**
     * HTTPS URL Keystone image, 1340x520 pixels.
     */
    @JsonProperty("keystone_image_2x")
    private String keystoneImage2x;

    /**
     * List of integers which identify ids of related images. With those image_ids,
     * it can be gathered more information using the image API call.
     */
    @JsonProperty("release_images")
    private List<Integer> releaseImages;

    /**
     * List of integers which identify ids of related videos. With those video_ids,
     * it can be gathered more information using the video API call.
     */
    @JsonProperty("release_videos")
    private List<Integer> releaseVideos;

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

    public ZonedDateTime getPublication() {
        return publication;
    }

    public void setPublication(ZonedDateTime publication) {
        this.publication = publication;
    }

    public String getMission() {
        return mission;
    }

    public void setMission(String mission) {
        this.mission = mission;
    }

    public String getAbstract() {
        return _abstract;
    }

    public void setAbstract(String _abstract) {
        this._abstract = _abstract;
    }

    public String getCredits() {
        return credits;
    }

    public void setCredits(String credits) {
        this.credits = credits;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getThumbnailRetina() {
        return thumbnailRetina;
    }

    public void setThumbnailRetina(String thumbnailRetina) {
        this.thumbnailRetina = thumbnailRetina;
    }

    public String getThumbnail1x() {
        return thumbnail1x;
    }

    public void setThumbnail1x(String thumbnail1x) {
        this.thumbnail1x = thumbnail1x;
    }

    public String getThumbnail2x() {
        return thumbnail2x;
    }

    public void setThumbnail2x(String thumbnail2x) {
        this.thumbnail2x = thumbnail2x;
    }

    public String getKeystoneImage1x() {
        return keystoneImage1x;
    }

    public void setKeystoneImage1x(String keystoneImage1x) {
        this.keystoneImage1x = keystoneImage1x;
    }

    public String getKeystoneImage2x() {
        return keystoneImage2x;
    }

    public void setKeystoneImage2x(String keystoneImage2x) {
        this.keystoneImage2x = keystoneImage2x;
    }

    public List<Integer> getReleaseImages() {
        return releaseImages;
    }

    public void setReleaseImages(List<Integer> releaseImages) {
        this.releaseImages = releaseImages;
    }

    public List<Integer> getReleaseVideos() {
        return releaseVideos;
    }

    public void setReleaseVideos(List<Integer> releaseVideos) {
        this.releaseVideos = releaseVideos;
    }

    @Override
    public String toString() {
        return "HubbleNasaNewsReleaseResponse [id=" + id + ", name=" + name + ", mission=" + mission + "]";
    }
}
