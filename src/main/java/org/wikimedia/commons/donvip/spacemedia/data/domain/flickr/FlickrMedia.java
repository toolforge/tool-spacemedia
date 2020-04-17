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
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Indexed
@Table(indexes = {@Index(columnList = "sha1")})
public class FlickrMedia extends Media<Long, LocalDateTime> {

    @Id
    @NotNull
    private Long id;

    @NotNull
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
    private Set<String> tags;

    @JsonProperty("originalformat")
    private String originalFormat;

    private double latitude;
    
    private double longitude;
    
    private double accuracy;

    @NotNull
    private String media;
    
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

    @NotNull
    @JsonProperty("pathalias")
    @Field(index = org.hibernate.search.annotations.Index.YES, analyze = Analyze.NO, store = Store.NO)
    private String pathAlias;

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

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

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

    public String getMedia() {
        return media;
    }

    public void setMedia(String media) {
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
        super.setAssetUrl(originalUrl);
        this.originalUrl = originalUrl;
    }

    @Override
    public void setAssetUrl(URL originalUrl) {
        super.setAssetUrl(originalUrl);
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
                + (getAssetUrl() != null ? "getAssetUrl()=" + getAssetUrl() : "") + "]";
    }
}
