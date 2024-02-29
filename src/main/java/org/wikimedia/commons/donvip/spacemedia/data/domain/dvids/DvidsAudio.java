package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import jakarta.persistence.Entity;

@Entity
public class DvidsAudio extends DvidsMedia {

    /**
     * Length of asset in seconds.
     */
    private Short duration;

    public Short getDuration() {
        return duration;
    }

    public void setDuration(Short duration) {
        this.duration = duration;
    }

    @Override
    public DvidsAudio copyDataFrom(DvidsMedia mediaFromApi) {
        super.copyDataFrom(mediaFromApi);
        if (mediaFromApi instanceof DvidsAudio audio) {
            setDuration(audio.getDuration());
        }
        return this;
    }
}
