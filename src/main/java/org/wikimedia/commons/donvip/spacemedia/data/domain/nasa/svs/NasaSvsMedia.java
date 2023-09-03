package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithKeywords;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api.NasaSvsMediaType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api.NasaSvsStudio;

@Entity
@Indexed
public class NasaSvsMedia extends Media implements WithKeywords {

    @Enumerated(EnumType.STRING)
    private NasaSvsMediaType type;

    @Enumerated(EnumType.STRING)
    private NasaSvsStudio studio;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> keywords = new HashSet<>();

    public NasaSvsMediaType getType() {
        return type;
    }

    public void setType(NasaSvsMediaType type) {
        this.type = type;
    }

    public NasaSvsStudio getStudio() {
        return studio;
    }

    public void setStudio(NasaSvsStudio studio) {
        this.studio = studio;
    }

    @Override
    public Set<String> getKeywords() {
        return keywords;
    }

    @Override
    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }

    public NasaSvsMedia copyDataFrom(NasaSvsMedia media) {
        super.copyDataFrom(media);
        setType(media.getType());
        setStudio(media.getStudio());
        return this;
    }
}
