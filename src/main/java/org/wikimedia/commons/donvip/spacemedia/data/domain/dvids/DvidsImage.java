package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.net.URL;

import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Indexed
public class DvidsImage extends DvidsMedia {

    /**
     * Aspect ratio of the asset.
     */
    @JsonProperty("aspect_ratio")
    private DvidsAspectRatio aspectRatio;

    @Transient
    public URL getImage() {
        return hasMetadata() ? getUniqueMetadata().getAssetUrl() : null;
    }

    @Transient
    public void setImage(URL imageUrl) {
        setAssetUrl(imageUrl);
    }

    public DvidsAspectRatio getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(DvidsAspectRatio aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    @Transient
    @JsonProperty("dimensions")
    public ImageDimensions getImageDimensions() {
        return getUniqueMetadata().getImageDimensions();
    }

    @Transient
    @JsonProperty("dimensions")
    public void setImageDimensions(ImageDimensions dimensions) {
        getUniqueMetadata().setImageDimensions(dimensions);
    }

    @Override
    public String toString() {
        return "DvidsImage ["
                + (getId() != null ? "id=" + getId() + ", " : "")
                + (getAspectRatio() != null ? "aspectRatio=" + getAspectRatio() + ", " : "")
                + (getTitle() != null ? "title=" + getTitle() + ", " : "")
                + (getDescription() != null ? "description=" + getDescription() : "") + "]";
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
