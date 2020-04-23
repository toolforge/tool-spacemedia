package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.net.URL;

import javax.persistence.Embedded;
import javax.persistence.Entity;

import org.hibernate.search.annotations.Indexed;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Indexed
public class DvidsImage extends DvidsMedia {

    /**
     * Aspect ratio of the asset.
     */
    @JsonProperty("aspect_ratio")
    private DvidsAspectRatio aspectRatio;

    /**
     * Original dimensions (width and height) of the asset
     */
    @Embedded
    private DvidsImageDimensions dimensions;

    @Override
    @JsonProperty("image")
    public URL getAssetUrl() {
        return super.getAssetUrl();
    }

    @Override
    @JsonProperty("image")
    public void setAssetUrl(URL assetUrl) {
        super.setAssetUrl(assetUrl);
    }

    public DvidsAspectRatio getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(DvidsAspectRatio aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public DvidsImageDimensions getDimensions() {
        return dimensions;
    }

    public void setDimensions(DvidsImageDimensions dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public String toString() {
        return "DvidsAudio ["
                + (getId() != null ? "id=" + getId() + ", " : "")
                + (getAspectRatio() != null ? "aspectRatio=" + getAspectRatio() + ", " : "")
                + (getTitle() != null ? "title=" + getTitle() + ", " : "")
                + (getDatePublished() != null ? "datePublished=" + getDatePublished() + ", " : "")
                + (getDate() != null ? "date=" + getDate() + ", " : "")
                + (getDescription() != null ? "description=" + getDescription() : "") + "]";
    }
}
