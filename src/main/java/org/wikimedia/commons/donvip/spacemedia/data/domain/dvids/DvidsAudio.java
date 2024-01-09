package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import jakarta.persistence.Entity;

@Entity
@Indexed
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
