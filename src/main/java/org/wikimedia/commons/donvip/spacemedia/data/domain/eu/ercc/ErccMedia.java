package org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc;

import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;

@Entity
public class ErccMedia extends Media {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EchoMapType mapType;

    @Column(nullable = true)
    private String sources;

    @Column(nullable = true, length = 64)
    private String eventType;

    @Column(nullable = true, length = 64)
    private String mainCountry;

    @Column(nullable = true, length = 64)
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> countries;

    @Column(nullable = true, length = 64)
    private String category;

    public EchoMapType getMapType() {
        return mapType;
    }

    public void setMapType(EchoMapType mapType) {
        this.mapType = mapType;
    }

    public String getSources() {
        return sources;
    }

    public void setSources(String sources) {
        this.sources = sources;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getMainCountry() {
        return mainCountry;
    }

    public void setMainCountry(String mainCountry) {
        this.mainCountry = mainCountry;
    }

    public Set<String> getCountries() {
        return countries;
    }

    public void setCountries(Set<String> countries) {
        this.countries = countries;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String getUploadTitle(FileMetadata fileMetadata) {
        return fileMetadata.getOriginalFileName().substring(0, fileMetadata.getOriginalFileName().indexOf('.'))
                .replace('_', ' ').trim();
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(category, countries, eventType, mainCountry, mapType, sources);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        ErccMedia other = (ErccMedia) obj;
        return Objects.equals(category, other.category) && Objects.equals(countries, other.countries)
                && Objects.equals(eventType, other.eventType) && Objects.equals(mainCountry, other.mainCountry)
                && mapType == other.mapType && Objects.equals(sources, other.sources);
    }

    @Override
    public String toString() {
        return "ErccMedia [id=" + getId() + ", mapType=" + mapType + ", sources=" + sources + ", eventType=" + eventType
                + ", mainCountry=" + mainCountry + ", countries=" + countries + ", category=" + category + ']';
    }

    public ErccMedia copyDataFrom(ErccMedia media) {
        super.copyDataFrom(media);
        setCategory(media.getCategory());
        setCountries(media.getCountries());
        setEventType(media.getEventType());
        setMainCountry(media.getMainCountry());
        setMapType(media.getMapType());
        setSources(media.getSources());
        return this;
    }
}
