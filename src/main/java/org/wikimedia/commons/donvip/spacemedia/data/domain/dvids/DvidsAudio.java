package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import javax.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

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
    public String toString() {
        return "DvidsAudio ["
                + (getId() != null ? "id=" + getId() + ", " : "")
                + (getTitle() != null ? "title=" + getTitle() + ", " : "")
                + (getDescription() != null ? "description=" + getDescription() : "") + "]";
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
