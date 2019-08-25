package org.wikimedia.commons.donvip.spacemedia.data.domain.eso;

import java.time.LocalDateTime;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMedia;

@MappedSuperclass
public abstract class CommonEsoMedia extends FullResMedia {

    @Id
    @Column(length = 127)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private EsoMediaType imageType;

    @Column(nullable = false)
    private LocalDateTime releaseDate;

    @Column(nullable = true, length = 127)
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> relatedAnnouncements;

    @Column(nullable = true, length = 127)
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> relatedReleases;

    private int width;
    private int height;

    @Column(nullable = true, length = 63)
    private String fieldOfView;

    @Column(nullable = true, length = 200)
    private String name;

    @Column(nullable = true, length = 63)
    private String distance;

    @Column(nullable = true, length = 127)
    private String constellation;

    @Column(nullable = true, length = 63)
    private String positionRa;

    @Column(nullable = true, length = 63)
    private String positionDec;

    @Column(nullable = true, length = 63)
    private String orientation;

    @Column(nullable = true, length = 63)
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> telescopes;

    @Column(nullable = false, length = 127)
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> types;

    @Column(nullable = false, length = 127)
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> categories;

    @Column(nullable = false, length = 375)
    private String credit;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public EsoMediaType getImageType() {
        return imageType;
    }

    public void setImageType(EsoMediaType imageType) {
        this.imageType = imageType;
    }

    public LocalDateTime getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDateTime releaseDate) {
        this.releaseDate = releaseDate;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
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

    @Override
    public String toString() {
        return "AbstractEsoMedia [" + (id != null ? "id=" + id + ", " : "")
                + (imageType != null ? "imageType=" + imageType + ", " : "")
                + (releaseDate != null ? "releaseDate=" + releaseDate + ", " : "") + "width=" + width + ", height="
                + height + ", " + (name != null ? "objectName=" + name + ", " : "")
                + (types != null ? "objectType=" + types + ", " : "")
                + (categories != null ? "objectCategories=" + categories + ", " : "")
                + (credit != null ? "credit=" + credit + ", " : "")
                + (fullResSha1 != null ? "fullResSha1=" + fullResSha1 + ", " : "")
                + (fullResAssetUrl != null ? "fullResAssetUrl=" + fullResAssetUrl + ", " : "")
                + (sha1 != null ? "sha1=" + sha1 + ", " : "") + (assetUrl != null ? "assetUrl=" + assetUrl + ", " : "")
                + (title != null ? "title=" + title + ", " : "")
                + (description != null ? "description=" + description + ", " : "")
                + (commonsFileNames != null ? "commonsFileNames=" + commonsFileNames + ", " : "")
                + (ignored != null ? "ignored=" + ignored + ", " : "")
                + (ignoredReason != null ? "ignoredReason=" + ignoredReason : "") + "]";
    }
}
