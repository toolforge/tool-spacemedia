package org.wikimedia.commons.donvip.spacemedia.data.domain.flickr;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService.normalizeFilename;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.getFirstSentence;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithKeywords;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithLatLon;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Transient;

@Entity
public class FlickrMedia extends Media implements WithLatLon, WithKeywords {

    private static final String STATICFLICKR_COM = ".staticflickr.com";

    public static final String USER_ID_STRING = "NHQ\\d{12}|GRC-\\d{4}-[A-Z]-\\d{5}|iss\\d{3}e\\d{6}|jsc\\d{4}e\\d{6}|[PV]\\d{8}[A-Z]{2}-\\d{4}";
    private static final Pattern USER_ID = Pattern.compile(".*(" + USER_ID_STRING + ").*", Pattern.CASE_INSENSITIVE);

    @Column(nullable = false)
    private int license;

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

    @JsonIgnoreProperties({ "pathAlias", "members" })
    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE,
            CascadeType.REFRESH }, mappedBy = "members")
    private Set<FlickrPhotoSet> photosets = new HashSet<>();

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

    @Transient
    @JsonIgnore
    public String getPathAlias() {
        return getId().getRepoId();
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
        if (m.matches()) {
            return Optional.of(m.group(1));
        } else if (isNotBlank(getDescription())) {
            m = USER_ID.matcher(getDescription());
            if (m.matches()) {
                return Optional.of(m.group(1));
            }
        }
        return Optional.empty();
    }

    @Override
    public String getUploadTitle(FileMetadata fileMetadata) {
        if ((title.isEmpty() || UnitedStates.isVirin(title) || UnitedStates.isFakeVirin(title))
                && isNotEmpty(getPhotosets())) {
            String albumTitle = getPhotosets().iterator().next().getTitle();
            if (isNotBlank(albumTitle)) {
                return albumTitle + " (" + getId().getMediaId() + ")";
            }
        }
        String s = getUploadTitle();
        return getUploadTitle(isTitleBlacklisted(s)
                ? normalizeFilename(getAlbumName().orElseGet(() -> getFirstSentence(getDescription(fileMetadata))))
                : s, getUserDefinedId().orElseGet(() -> getUploadId(fileMetadata)));
    }

    @Override
    public List<String> getAlbumNames() {
        return getPhotosets().stream().map(FlickrPhotoSet::getTitle).toList();
    }

    @Override
    public String toString() {
        return "FlickrMedia [" + (getId() != null ? "id=" + getId() + ", " : "")
                + (title != null ? "title=" + title + ", " : "")
                + "license=" + license + ", " + "metadata=" + getMetadata() + "]";
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
        this.dateTakenGranularity = mediaFromApi.dateTakenGranularity;
        this.originalFormat = mediaFromApi.originalFormat;
        this.latitude = mediaFromApi.latitude;
        this.longitude = mediaFromApi.longitude;
        this.accuracy = mediaFromApi.accuracy;
        this.media = mediaFromApi.media;
        this.mediaStatus = mediaFromApi.mediaStatus;
        return this;
    }
}
