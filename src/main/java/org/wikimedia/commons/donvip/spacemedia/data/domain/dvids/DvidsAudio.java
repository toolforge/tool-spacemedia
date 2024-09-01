package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

import java.net.URL;
import java.util.Objects;
import java.time.Duration;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaDimensions;

@Entity
public class DvidsAudio extends DvidsMedia {

    /**
     * Length of asset in seconds.
     */
    private Short duration;

    private URL file;

    public Short getDuration() {
        return duration;
    }

    public void setDuration(Short duration) {
        this.duration = duration;
    }

    public URL getFile() {
        return file;
    }

    public void setFile(URL file) {
        this.file = file;
    }

    @Override
    @Transient
    public URL getAssetUrl() {
        return getFile();
    }

    @Override
    @Transient
    public MediaDimensions getMediaDimensions() {
        return new MediaDimensions(0, 0, Duration.ofSeconds(duration));
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(duration, file);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        DvidsAudio other = (DvidsAudio) obj;
        return Objects.equals(duration, other.duration) && Objects.equals(file, other.file);
    }

    @Override
    public DvidsAudio copyDataFrom(DvidsMedia mediaFromApi) {
        super.copyDataFrom(mediaFromApi);
        if (mediaFromApi instanceof DvidsAudio audio) {
            setDuration(audio.getDuration());
            setFile(audio.getFile());
        }
        return this;
    }
}
