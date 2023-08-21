package org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
public class DjangoplicityMedia extends Media<DjangoplicityMediaId> {

    @Id
    @Embedded
    @DocumentId(identifierBridge = @IdentifierBridgeRef(type = DjangoplicityMediaIdBridge.class))
    private DjangoplicityMediaId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 16)
    private DjangoplicityLicence licence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 16)
    @JsonProperty("image_type")
    private DjangoplicityMediaType imageType;

    @Column(nullable = true, length = 127)
    @ElementCollection(fetch = FetchType.EAGER)
    @JsonProperty("related_announcements")
    private Set<String> relatedAnnouncements = new HashSet<>();

    @Column(nullable = true, length = 127)
    @ElementCollection(fetch = FetchType.EAGER)
    @JsonProperty("related_releases")
    private Set<String> relatedReleases = new HashSet<>();

    @Column(nullable = true, length = 63)
    @JsonProperty("field_of_view")
    private String fieldOfView;

    // 541 chars for
    // https://noirlab.edu/public/images/Cerro-Tololo-Drone-Pano_12-CC/
    @Lob
    @Column(nullable = true, columnDefinition = "TEXT")
    private String name;

    @Column(nullable = true, length = 63)
    private String distance;

    @Column(nullable = true, length = 127)
    private String constellation;

    @Column(nullable = true, length = 63)
    @JsonProperty("position_ra")
    private String positionRa;

    @Column(nullable = true, length = 63)
    @JsonProperty("position_dec")
    private String positionDec;

    @Column(nullable = true, length = 63)
    private String orientation;

    @Column(nullable = true, length = 63)
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> telescopes = new HashSet<>();

    @Column(nullable = true, length = 63)
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> instruments = new HashSet<>();

    @Column(nullable = false, length = 127)
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> types = new HashSet<>();

    @Column(nullable = false, length = 127)
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> categories = new HashSet<>();

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String credit;

    @Override
    public DjangoplicityMediaId getId() {
        return id;
    }

    @Override
    public void setId(DjangoplicityMediaId id) {
        this.id = id;
    }

    @Override
    public String getIdUsedInOrg() {
        return getId().getId();
    }

    public DjangoplicityLicence getLicence() {
        return licence;
    }

    public void setLicence(DjangoplicityLicence licence) {
        this.licence = licence;
    }

    public DjangoplicityMediaType getImageType() {
        return imageType;
    }

    public void setImageType(DjangoplicityMediaType imageType) {
        this.imageType = imageType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getConstellation() {
        return constellation;
    }

    public void setConstellation(String constellation) {
        this.constellation = constellation;
    }

    public Set<String> getTypes() {
        return types;
    }

    public void setTypes(Set<String> types) {
        this.types = types;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }

    public String getCredit() {
        return credit;
    }

    public void setCredit(String credit) {
        this.credit = credit;
    }

    public String getFieldOfView() {
        return fieldOfView;
    }

    public void setFieldOfView(String fieldOfView) {
        this.fieldOfView = fieldOfView;
    }

    public Set<String> getRelatedReleases() {
        return relatedReleases;
    }

    public void setRelatedReleases(Set<String> relatedReleases) {
        this.relatedReleases = relatedReleases;
    }

    public Set<String> getRelatedAnnouncements() {
        return relatedAnnouncements;
    }

    public void setRelatedAnnouncements(Set<String> relatedAnnouncements) {
        this.relatedAnnouncements = relatedAnnouncements;
    }

    public String getPositionRa() {
        return positionRa;
    }

    public void setPositionRa(String positionRa) {
        this.positionRa = positionRa;
    }

    public String getPositionDec() {
        return positionDec;
    }

    public void setPositionDec(String positionDec) {
        this.positionDec = positionDec;
    }

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

    public Set<String> getTelescopes() {
        return telescopes;
    }

    public void setTelescopes(Set<String> telescopes) {
        this.telescopes = telescopes;
    }

    public Set<String> getInstruments() {
        return instruments;
    }

    public void setInstruments(Set<String> instruments) {
        this.instruments = instruments;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + (id != null ? "id=" + id + ", " : "")
                + (licence != null ? "licence=" + licence + ", " : "")
                + (imageType != null ? "imageType=" + imageType + ", " : "")
                + (name != null ? "name=" + name + ", " : "")
                + (isNotEmpty(types) ? "types=" + types + ", " : "")
                + (isNotEmpty(categories) ? "categories=" + categories + ", " : "")
                + (credit != null ? "credit=" + credit.replace('\n', ' ') + ", " : "")
                + (getMetadata() != null ? "metadata=" + getMetadata() + ", " : "")
                + (title != null ? "title=" + title + ", " : "")
                + (description != null ? "description=" + description + ", " : "")
                + (isNotEmpty(telescopes) ? "telescopes=" + telescopes + ", " : "")
                + (isNotEmpty(instruments) ? "instruments=" + instruments + ", " : "")
                + (ignored != null ? "ignored=" + ignored + ", " : "")
                + (ignoredReason != null ? "ignoredReason=" + ignoredReason : "") + "]";
    }

    @Override
    public boolean isAudio() {
        return false;
    }

    @Override
    public boolean isImage() {
        return true;
    }

    @Override
    public boolean isVideo() {
        return false;
    }

    public DjangoplicityMedia copyDataFrom(DjangoplicityMedia mediaFromApi) {
        super.copyDataFrom(mediaFromApi);
        this.categories = mediaFromApi.categories;
        this.constellation = mediaFromApi.constellation;
        this.credit = mediaFromApi.credit;
        this.distance = mediaFromApi.distance;
        this.fieldOfView = mediaFromApi.fieldOfView;
        this.imageType = mediaFromApi.imageType;
        this.licence = mediaFromApi.licence;
        this.name = mediaFromApi.name;
        this.orientation = mediaFromApi.orientation;
        this.positionDec = mediaFromApi.positionDec;
        this.positionRa = mediaFromApi.positionRa;
        this.relatedAnnouncements = mediaFromApi.relatedAnnouncements;
        this.relatedReleases = mediaFromApi.relatedReleases;
        this.telescopes = mediaFromApi.telescopes;
        this.instruments = mediaFromApi.instruments;
        this.types = mediaFromApi.types;
        return this;
    }
}
