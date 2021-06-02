package org.wikimedia.commons.donvip.spacemedia.data.jpa.entity;

import java.time.Duration;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class AudioFile extends File implements Temporal {

    @Column(nullable = false)
    private Duration duration;

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
        return 31 * super.hashCode() + Objects.hash(duration);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        AudioFile other = (AudioFile) obj;
        return Objects.equals(duration, other.duration);
    }
}
