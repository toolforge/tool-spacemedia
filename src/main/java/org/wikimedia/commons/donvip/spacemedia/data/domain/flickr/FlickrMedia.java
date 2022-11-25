package org.wikimedia.commons.donvip.spacemedia.data.domain.flickr;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Indexed
@Table(indexes = { @Index(columnList = "sha1, phash") })
public class FlickrMedia extends Media<Long, LocalDateTime> {

    @Id
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false)
    private int license;

    @JsonProperty("dateupload")
    @Column(name = "date_upload", nullable = false)
    private LocalDateTime date;

    @JsonProperty("lastupdate")
    private LocalDateTime lastUpdate;

    @JsonProperty("datetaken")
    private LocalDateTime dateTaken;

    @JsonProperty("datetakengranularity")
    private int dateTakenGranularity;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> tags = new HashSet<>();

    @JsonProperty("originalformat")
    private String originalFormat;

    private double latitude;

    private double longitude;

    private double accuracy;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FlickrMediaType media;

    @JsonProperty("media_status")
    private String mediaStatus;

    @Transient
    @JsonInclude
    @JsonProperty("url_o")
    private URL originalUrl;

    @JsonProperty("height_o")
    private int originalHeight;

    @JsonProperty("width_o")
    private int originalWidth;

    @Column(nullable = false)
    @JsonProperty("pathalias")
    private String pathAlias;

    @JsonIgnoreProperties({ "pathAlias", "members" })
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, mappedBy = "members")
    private Set<FlickrPhotoSet> photosets = new HashSet<>();

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public int getLicense() {
        return license;
    }

    public void setLicense(int license) {
        this.license = license;
    }

    @Override
    public LocalDateTime getDate() {
        return date;
    }

    @Override
    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    @Override
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    @Override
    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public LocalDateTime getDateTaken() {
        return dateTaken;
    }

    public void setDateTaken(LocalDateTime dateTaken) {
        this.dateTaken = dateTaken;
    }

    public int getDateTakenGranularity() {
        return dateTakenGranularity;
    }

    public void setDateTakenGranularity(int dateTakenGranularity) {
        this.dateTakenGranularity = dateTakenGranularity;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getOriginalFormat() {
        return originalFormat;
    }

    public void setOriginalFormat(String originalFormat) {
        this.originalFormat = originalFormat;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public FlickrMediaType getMedia() {
        return media;
    }

    public void setMedia(FlickrMediaType media) {
        this.media = media;
    }

    public String getMediaStatus() {
        return mediaStatus;
    }

    public void setMediaStatus(String mediaStatus) {
        this.mediaStatus = mediaStatus;
    }

    public URL getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(URL originalUrl) {
        metadata.setAssetUrl(originalUrl);
        this.originalUrl = originalUrl;
    }

    public int getOriginalHeight() {
        return originalHeight;
    }

    public void setOriginalHeight(int originalHeight) {
        this.originalHeight = originalHeight;
    }

    public int getOriginalWidth() {
        return originalWidth;
    }

    public void setOriginalWidth(int originalWidth) {
        this.originalWidth = originalWidth;
    }

    public String getPathAlias() {
        return pathAlias;
    }

    public void setPathAlias(String pathAlias) {
        this.pathAlias = pathAlias;
    }

    public Set<FlickrPhotoSet> getPhotosets() {
        return photosets;
    }

    public void addPhotoSet(FlickrPhotoSet photoset) {
        this.photosets.add(photoset);
        photoset.getMembers().add(this);
    }

    public void removePhotoSet(FlickrPhotoSet photoset) {
        this.photosets.remove(photoset);
        photoset.getMembers().remove(this);
    }

    @Override
    public String getUploadTitle() {
        if ((UnitedStates.isVirin(title) || UnitedStates.isFakeVirin(title))
                && CollectionUtils.isNotEmpty(getPhotosets())) {
            String albumTitle = getPhotosets().iterator().next().getTitle();
            if (StringUtils.isNotBlank(albumTitle)) {
                return albumTitle + " (" + getId() + ")";
            }
        }
        return super.getUploadTitle();
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(id, pathAlias);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        FlickrMedia other = (FlickrMedia) obj;
        return Objects.equals(id, other.id) && Objects.equals(pathAlias, other.pathAlias);
    }

    @Override
    public String toString() {
        return "FlickrMedia [" + (id != null ? "id=" + id + ", " : "") + (title != null ? "title=" + title + ", " : "")
                + "license=" + license + ", " + (date != null ? "date=" + date + ", " : "")
                + (pathAlias != null ? "pathAlias=" + pathAlias + ", " : "")
                + "metadata=" + metadata + "]";
    }

    @Override
    public boolean isAudio() {
        return false;
    }

    @Override
    public boolean isImage() {
        return media == FlickrMediaType.photo;
    }

    @Override
    public boolean isVideo() {
        return media == FlickrMediaType.video;
    }

    public FlickrMedia copyDataFrom(FlickrMedia mediaFromApi) {
        super.copyDataFrom(mediaFromApi);
        this.license = mediaFromApi.license;
        this.date = mediaFromApi.date;
        this.lastUpdate = mediaFromApi.lastUpdate;
        this.dateTaken = mediaFromApi.dateTaken;
        this.dateTakenGranularity = mediaFromApi.dateTakenGranularity;
        this.tags = mediaFromApi.tags;
        this.originalFormat = mediaFromApi.originalFormat;
        this.latitude = mediaFromApi.latitude;
        this.longitude = mediaFromApi.longitude;
        this.accuracy = mediaFromApi.accuracy;
        this.media = mediaFromApi.media;
        this.mediaStatus = mediaFromApi.mediaStatus;
        this.originalUrl = mediaFromApi.originalUrl;
        this.originalHeight = mediaFromApi.originalHeight;
        this.originalWidth = mediaFromApi.originalWidth;
        // Do not override pathAlias !
        this.metadata.setAssetUrl(originalUrl);
        return this;
    }
}
