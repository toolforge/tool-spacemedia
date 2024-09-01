package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.time.Duration;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;

@Embeddable
public class MediaDimensions {
    /**
     * Height of original image, in pixels.
     */
    private Integer height;

    /**
     * Width of original image, in pixels.
     */
    private Integer width;

    /**
     * Audio/video duration. Can be null
     */
    @Column(nullable = true)
    private Duration duration;

    public MediaDimensions() {

    }

    public MediaDimensions(Integer width, Integer height) {
        this(width, height, null);
    }

    public MediaDimensions(Integer width, Integer height, Duration duration) {
        this.width = width;
        this.height = height;
        this.duration = duration;
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

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    @Transient
    public long getPixelsNumber() {
        return width * (long) height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(height, width, duration);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        MediaDimensions other = (MediaDimensions) obj;
        return Objects.equals(height, other.height) && Objects.equals(width, other.width) && Objects.equals(duration, other.duration);
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
    public boolean hasValidDuration() {
        return duration != null && duration.toNanos() > 0;
    }

    @Transient
    @JsonIgnore
    public double getAspectRatio() {
        return (double) width / (double) height;
    }
}
