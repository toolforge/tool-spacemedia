package org.wikimedia.commons.donvip.spacemedia.data.jpa.entity;

import java.time.Duration;
import java.util.Objects;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(value = "V")
public class VideoFile extends File implements Visual, Temporal {

    private int height;

    private int width;

    private Duration duration;

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public Duration getDuration() {
        return duration;
    }

    @Override
    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(duration, height, width);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        VideoFile other = (VideoFile) obj;
        return Objects.equals(duration, other.duration) && height == other.height && width == other.width;
    }

}
