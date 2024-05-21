package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.chandra;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;

@Entity
public class NasaChandraMedia extends Media {

    @Column(nullable = true, length = 255)
    private String scale;

    @Column(nullable = true, length = 127)
    private String category;

    @Column(nullable = true, length = 127)
    private String coordinates;

    @Column(nullable = true, length = 127)
    private String constellation;

    @Column(nullable = true, length = 127)
    private String distance;

    @Column(nullable = true, length = 255)
    private String observationDate;

    @Column(nullable = true, length = 127)
    private String observationTime;

    @Lob
    @Column(nullable = true, columnDefinition = "TEXT")
    private String observationIds;

    @Column(nullable = true, length = 63)
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> instruments = new HashSet<>();

    @Lob
    @Column(nullable = true, columnDefinition = "TEXT")
    private String references;

    @Lob
    @Column(nullable = true, columnDefinition = "TEXT")
    private String colorCode;

    @Column(nullable = true, length = 127)
    private String alsoKnownAs;

    @Lob
    @Column(nullable = true, columnDefinition = "TEXT")
    private String aboutTheSound;

    public String getScale() {
        return scale;
    }

    public void setScale(String scale) {
        this.scale = scale;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(String coordinates) {
        this.coordinates = coordinates;
    }

    public String getConstellation() {
        return constellation;
    }

    public void setConstellation(String constellation) {
        this.constellation = constellation;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getObservationDate() {
        return observationDate;
    }

    public void setObservationDate(String observationDate) {
        this.observationDate = observationDate;
    }

    public String getObservationTime() {
        return observationTime;
    }

    public void setObservationTime(String observationTime) {
        this.observationTime = observationTime;
    }

    public String getObservationIds() {
        return observationIds;
    }

    public void setObservationIds(String observationIds) {
        this.observationIds = observationIds;
    }

    public Set<String> getInstruments() {
        return instruments;
    }

    public void setInstruments(Set<String> instruments) {
        this.instruments = instruments;
    }

    public String getReferences() {
        return references;
    }

    public void setReferences(String references) {
        this.references = references;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public String getAlsoKnownAs() {
        return alsoKnownAs;
    }

    public void setAlsoKnownAs(String alsoKnownAs) {
        this.alsoKnownAs = alsoKnownAs;
    }

    public String getAboutTheSound() {
        return aboutTheSound;
    }

    public void setAboutTheSound(String aboutTheSound) {
        this.aboutTheSound = aboutTheSound;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(category, colorCode, constellation, coordinates, distance,
                instruments, observationDate, observationIds, observationTime, references, scale, alsoKnownAs,
                aboutTheSound);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        NasaChandraMedia other = (NasaChandraMedia) obj;
        return Objects.equals(category, other.category) && Objects.equals(colorCode, other.colorCode)
                && Objects.equals(constellation, other.constellation) && Objects.equals(coordinates, other.coordinates)
                && Objects.equals(distance, other.distance) && Objects.equals(instruments, other.instruments)
                && Objects.equals(observationDate, other.observationDate)
                && Objects.equals(observationIds, other.observationIds)
                && Objects.equals(observationTime, other.observationTime)
                && Objects.equals(references, other.references) && Objects.equals(scale, other.scale)
                && Objects.equals(alsoKnownAs, other.alsoKnownAs) && Objects.equals(aboutTheSound, other.aboutTheSound);
    }

    @Override
    public String toString() {
        return "NasaChandraMedia [publicationDate=" + getPublicationDate() + ", id=" + getId() + ']';
    }

    public NasaChandraMedia copyDataFrom(NasaChandraMedia other) {
        super.copyDataFrom(other);
        setAboutTheSound(other.getAboutTheSound());
        setAlsoKnownAs(other.getAlsoKnownAs());
        setCategory(other.getCategory());
        setColorCode(other.getColorCode());
        setConstellation(other.getConstellation());
        setCoordinates(other.getCoordinates());
        setDistance(other.getDistance());
        setInstruments(other.getInstruments());
        setObservationDate(other.getObservationDate());
        setObservationIds(other.getObservationIds());
        setObservationTime(other.getObservationTime());
        setReferences(other.getReferences());
        setScale(other.getScale());
        return this;
    }
}
