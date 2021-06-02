package org.wikimedia.commons.donvip.spacemedia.data.jpa.entity;

import java.time.Duration;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class VideoFile extends File implements Visual, Temporal {

    @Column(nullable = false)
    private short height;

    @Column(nullable = false)
    private short width;

    @Column(nullable = false)
    private Duration duration;

    @Override
    public short getHeight() {
        return height;
    }

    @Override
    public void setHeight(short height) {
        this.height = height;
    }

    @Override
    public short getWidth() {
        return width;
    }

    @Override
    public void setWidth(short width) {
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
