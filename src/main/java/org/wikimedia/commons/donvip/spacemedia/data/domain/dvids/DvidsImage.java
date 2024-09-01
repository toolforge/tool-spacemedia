package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.net.URL;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaDimensions;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

@Entity
public class DvidsImage extends DvidsMedia {

    /**
     * Aspect ratio of the asset.
     */
    @JsonProperty("aspect_ratio")
    private DvidsAspectRatio aspectRatio;

    @Transient
    private URL image;

    @Transient
    private MediaDimensions dimensions;

    @Transient
    public URL getImage() {
        return image;
    }

    @Transient
    public void setImage(URL imageUrl) {
        this.image = imageUrl;
    }

    public DvidsAspectRatio getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(DvidsAspectRatio aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    @Transient
    @JsonProperty("dimensions")
    public MediaDimensions getImageDimensions() {
        return dimensions;
    }

    @Transient
    @JsonProperty("dimensions")
    public void setImageDimensions(MediaDimensions dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    @Transient
    public URL getAssetUrl() {
        return getImage();
    }

    @Override
    @Transient
    public MediaDimensions getMediaDimensions() {
        return getImageDimensions();
    }

    @Override
    public DvidsImage copyDataFrom(DvidsMedia mediaFromApi) {
        super.copyDataFrom(mediaFromApi);
        if (mediaFromApi instanceof DvidsImage imgFromApi) {
            setAspectRatio(imgFromApi.getAspectRatio());
            setImageDimensions(imgFromApi.getImageDimensions());
            setImage(imgFromApi.getImage());
        }
        return this;
    }
}
