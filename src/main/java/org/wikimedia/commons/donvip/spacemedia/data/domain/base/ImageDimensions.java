package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;

@Embeddable
public class ImageDimensions {
    /**
     * Height of original image.
     */
    private Integer height;

    /**
     * Width of original image.
     */
    private Integer width;

    public ImageDimensions() {

    }

    public ImageDimensions(Integer width, Integer height) {
        this.width = Objects.requireNonNull(width, "width");
        this.height = Objects.requireNonNull(height, "height");
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public long getPixelsNumber() {
        return width * (long) height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(height, width);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        ImageDimensions other = (ImageDimensions) obj;
        return Objects.equals(height, other.height) && Objects.equals(width, other.width);
    }

    @Override
    public String toString() {
        return "[width=" + width + ", height=" + height + ']';
    }

    @Transient
    @JsonIgnore
    public boolean isValid() {
        return height != null && width != null && height > 0 && width > 0;
    }

    @Transient
    @JsonIgnore
    public double getAspectRatio() {
        return (double) width / (double) height;
    }
}
