package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithKeywords;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api.NasaSvsStudio;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;

@Entity
@Indexed
public class NasaSvsMedia extends Media implements WithKeywords {

    @Enumerated(EnumType.STRING)
    private NasaSvsStudio studio;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> keywords = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> missions = new HashSet<>();

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

    public Set<String> getMissions() {
        return missions;
    }

    public void setMissions(Set<String> missions) {
        this.missions = missions;
    }

    @Override
    public List<String> getIdUsedInCommons() {
        return List.of("SVS" + getIdUsedInOrg()); // Simple ids cannot be used as search discriminant
    }

    @Override
    protected String getUploadId(FileMetadata fileMetadata) {
        return "SVS" + super.getUploadId(fileMetadata);
    }

    public NasaSvsMedia copyDataFrom(NasaSvsMedia media) {
        super.copyDataFrom(media);
        setStudio(media.getStudio());
        setMissions(media.getMissions());
        return this;
    }

    @Override
    public String toString() {
        return "NasaSvsMedia [studio=" + studio + ", keywords=" + keywords + ", missions=" + missions + ", title="
                + title + ", publicationDate=" + publicationDate + ", id=" + getIdUsedInOrg() + "]";
    }
}
