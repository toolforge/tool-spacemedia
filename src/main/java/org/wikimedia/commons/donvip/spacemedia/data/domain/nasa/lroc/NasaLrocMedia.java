package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.lroc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithKeywords;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;

@Entity
@Indexed
public class NasaLrocMedia extends Media implements WithKeywords {

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> keywords = new HashSet<>();

    @Override
    public Set<String> getKeywords() {
        return keywords;
    }

    @Override
    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }

    @Override
    public List<String> getIdUsedInCommons() {
        return List.of("LROC" + getIdUsedInOrg()); // Simple ids cannot be used as search discriminant
    }

    @Override
    protected String getUploadId(FileMetadata fileMetadata) {
        return "LROC" + super.getUploadId(fileMetadata);
    }

    public NasaLrocMedia copyDataFrom(NasaLrocMedia other) {
        super.copyDataFrom(other);
        return this;
    }

    @Override
    public String toString() {
        return "NasaLrocMedia [id=" + getId() + ']';
    }
}
