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
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> eventTypes;

    @Column(nullable = true, length = 64)
    private String continent;

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

    public Set<String> getEventTypes() {
        return eventTypes;
    }

    public void setEventTypes(Set<String> eventTypes) {
        this.eventTypes = eventTypes;
    }

    public String getContinent() {
        return continent;
    }

    public void setContinent(String continent) {
        this.continent = continent;
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
        String fileName = fileMetadata.getOriginalFileName();
        return fileName != null ? fileName.substring(0, fileName.indexOf('.')).replace('_', ' ').trim()
                : super.getUploadId(fileMetadata);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode()
                + Objects.hash(category, countries, continent, eventTypes, mainCountry, mapType, sources);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        ErccMedia other = (ErccMedia) obj;
        return Objects.equals(category, other.category) && Objects.equals(countries, other.countries)
                && Objects.equals(continent, other.continent) && Objects.equals(eventTypes, other.eventTypes)
                && Objects.equals(mainCountry, other.mainCountry) && mapType == other.mapType
                && Objects.equals(sources, other.sources);
    }

    @Override
    public String toString() {
        return "ErccMedia [id=" + getId() + ", mapType=" + mapType + ", sources=" + sources + ", eventTypes="
                + eventTypes + ", continent=" + continent + ", mainCountry=" + mainCountry + ", countries=" + countries
                + ", category=" + category + ']';
    }

    public ErccMedia copyDataFrom(ErccMedia media) {
        super.copyDataFrom(media);
        setCategory(media.getCategory());
        setContinent(media.getContinent());
        setCountries(media.getCountries());
        setEventTypes(media.getEventTypes());
        setMainCountry(media.getMainCountry());
        setMapType(media.getMapType());
        setSources(media.getSources());
        return this;
    }
}
