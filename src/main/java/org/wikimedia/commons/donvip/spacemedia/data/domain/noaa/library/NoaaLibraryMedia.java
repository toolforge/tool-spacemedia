package org.wikimedia.commons.donvip.spacemedia.data.domain.noaa.library;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithKeywords;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;

@Entity
public class NoaaLibraryMedia extends Media implements WithKeywords {

    @Column
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

    public NoaaLibraryMedia copyDataFrom(NoaaLibraryMedia other) {
        super.copyDataFrom(other);
        return this;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(keywords);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        NoaaLibraryMedia other = (NoaaLibraryMedia) obj;
        return Objects.equals(keywords, other.keywords);
    }

    @Override
    public String toString() {
        return "NoaaLibraryMedia [publicationDate=" + getPublicationDate() + ", id=" + getId() + ']';
    }
}
