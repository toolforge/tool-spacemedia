package org.wikimedia.commons.donvip.spacemedia.data.domain.flickr;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.SingleFileMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithKeywords;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithLatLon;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Indexed
public class FlickrMedia extends SingleFileMedia<Long> implements WithLatLon, WithKeywords {

    private static final String STATICFLICKR_COM = ".staticflickr.com";

    private static final Pattern USER_ID = Pattern.compile(".*(NHQ\\d{12}|GRC-\\d{4}-[A-Z]-\\d{5}).*");

    @Id
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false)
    private int license;

    @JsonProperty("lastupdate")
    private LocalDateTime lastUpdate;

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

    @Column(nullable = false)
    @JsonProperty("pathalias")
    private String pathAlias;

    @JsonIgnoreProperties({ "pathAlias", "members" })
    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE,
            CascadeType.REFRESH }, mappedBy = "members")
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
    @JsonProperty("datetaken")
    public ZonedDateTime getCreationDateTime() {
        return super.getCreationDateTime();
    }

    @Override
    @JsonProperty("datetaken")
    public void setCreationDateTime(ZonedDateTime creationDateTime) {
        super.setCreationDateTime(creationDateTime);
    }

    @Override
    @JsonProperty("dateupload")
    public ZonedDateTime getPublicationDateTime() {
        return super.getPublicationDateTime();
    }

    @Override
    @JsonProperty("dateupload")
    public void setPublicationDateTime(ZonedDateTime publicationDateTime) {
        super.setPublicationDateTime(publicationDateTime);
    }

    @Override
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    @Override
    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
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

    @Override
    @Transient
    @JsonIgnore
    public Set<String> getKeywords() {
        return getTags();
    }

    @Override
    @Transient
    @JsonIgnore
    public void setKeywords(Set<String> tags) {
        setTags(tags);
    }

    public String getOriginalFormat() {
        return originalFormat;
    }

    public void setOriginalFormat(String originalFormat) {
        this.originalFormat = originalFormat;
    }

    @Override
    public double getLatitude() {
        return latitude;
    }

    @Override
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    @Override
    public double getLongitude() {
        return longitude;
    }

    @Override
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public double getPrecision() {
        return GlobeCoordinatesValue.PREC_MILLI_ARCSECOND;
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
        getPhotosets().add(photoset);
        photoset.getMembers().add(this);
    }

    public void removePhotoSet(FlickrPhotoSet photoset) {
        getPhotosets().remove(photoset);
        photoset.getMembers().remove(this);
    }

    @Override
    public List<String> getIdUsedInCommons() {
        List<String> result = new ArrayList<>(super.getIdUsedInCommons());
        getUserDefinedId().ifPresent(result::add);
        return result;
    }

    @Transient
    @JsonIgnore
    public Optional<String> getUserDefinedId() {
        Matcher m = USER_ID.matcher(getTitle());
        return m.matches() ? Optional.of(m.group(1)) : Optional.empty();
    }

    @Override
    public String getUploadTitle(FileMetadata fileMetadata) {
        if (title.isEmpty() || (UnitedStates.isVirin(title) || UnitedStates.isFakeVirin(title))
                && CollectionUtils.isNotEmpty(getPhotosets())) {
            String albumTitle = getPhotosets().iterator().next().getTitle();
            if (StringUtils.isNotBlank(albumTitle)) {
                return albumTitle + " (" + getId() + ")";
            }
        }
        return getUploadTitle(getUploadTitle(), getUserDefinedId().orElseGet(() -> getUploadId(fileMetadata)));
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
                + "license=" + license + ", " + (pathAlias != null ? "pathAlias=" + pathAlias + ", " : "")
                + "metadata=" + getMetadata() + "]";
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

    @Override
    protected boolean areSameUris(URI a, URI b) {
        return super.areSameUris(a, b)
                || (a.getHost().endsWith(STATICFLICKR_COM) && b.getHost().endsWith(STATICFLICKR_COM)
                        && a.getPath().equals(b.getPath()));
    }

    public FlickrMedia copyDataFrom(FlickrMedia mediaFromApi) {
        super.copyDataFrom(mediaFromApi);
        this.license = mediaFromApi.license;
        this.lastUpdate = mediaFromApi.lastUpdate;
        this.dateTakenGranularity = mediaFromApi.dateTakenGranularity;
        this.tags = mediaFromApi.tags;
        this.originalFormat = mediaFromApi.originalFormat;
        this.latitude = mediaFromApi.latitude;
        this.longitude = mediaFromApi.longitude;
        this.accuracy = mediaFromApi.accuracy;
        this.media = mediaFromApi.media;
        this.mediaStatus = mediaFromApi.mediaStatus;
        // Do not override pathAlias !
        return this;
    }
}
