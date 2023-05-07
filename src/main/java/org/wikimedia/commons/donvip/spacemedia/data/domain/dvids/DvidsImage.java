package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.net.URL;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.WithDimensions;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Indexed
public class DvidsImage extends DvidsMedia implements WithDimensions {

    /**
     * Aspect ratio of the asset.
     */
    @JsonProperty("aspect_ratio")
    private DvidsAspectRatio aspectRatio;

    /**
     * Original dimensions (width and height) of the asset
     */
    @Embedded
    private ImageDimensions dimensions;

    @Transient
    public URL getImage() {
        return metadata.getAssetUrl();
    }

    @Transient
    public void setImage(URL imageUrl) {
        metadata.setAssetUrl(imageUrl);
    }

    public DvidsAspectRatio getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(DvidsAspectRatio aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    @Override
    public ImageDimensions getImageDimensions() {
        return dimensions;
    }

    @Override
    public void setImageDimensions(ImageDimensions dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public String toString() {
        return "DvidsImage ["
                + (getId() != null ? "id=" + getId() + ", " : "")
                + (getAspectRatio() != null ? "aspectRatio=" + getAspectRatio() + ", " : "")
                + (getTitle() != null ? "title=" + getTitle() + ", " : "")
                + (getDatePublished() != null ? "datePublished=" + getDatePublished() + ", " : "")
                + (getDate() != null ? "date=" + getDate() + ", " : "")
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
