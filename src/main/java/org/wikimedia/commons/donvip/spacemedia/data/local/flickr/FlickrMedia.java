package org.wikimedia.commons.donvip.spacemedia.data.local.flickr;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.validation.constraints.NotNull;

import org.wikimedia.commons.donvip.spacemedia.data.local.Media;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
public class FlickrMedia extends Media {

    @Id
    @NotNull
    Long id;

    @NotNull
    String title;

    @NotNull
    int license; 

    @Lob
    String description;

    @NotNull
    @JsonProperty("dateupload")
    LocalDateTime dateUpload;

    @JsonProperty("lastupdate")
    LocalDateTime lastUpdate;

    @JsonProperty("datetaken")
    LocalDateTime dateTaken;

    @JsonProperty("datetakengranularity")
    int dateTakenGranularity;// ": 0
    
    @ElementCollection(fetch = FetchType.EAGER)
    Set<String> tags;

    @JsonProperty("originalformat")
    String originalFormat;
    
    double latitude;
    
    double longitude;
    
    double accuracy;

    @NotNull
    String media;
    
    @JsonProperty("media_status")
    String mediaStatus;// ": "ready"

    @JsonProperty("height_o")
    int originalHeight;

    @JsonProperty("width_o")
    int originalWidth;

    @NotNull
    @JsonProperty("pathalias")
    String pathAlias;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getLicense() {
        return license;
    }

    public void setLicense(int license) {
        this.license = license;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateUpload() {
        return dateUpload;
    }

    public void setDateUpload(LocalDateTime dateUpload) {
        this.dateUpload = dateUpload;
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

    @Override
    @JsonProperty("url_o")
    public URL getAssetUrl() {
        return super.getAssetUrl();
    }

    @Override
    @JsonProperty("url_o")
    public void setAssetUrl(URL originalUrl) {
        super.setAssetUrl(originalUrl);
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
                + "license=" + license + ", " + (dateUpload != null ? "dateUpload=" + dateUpload + ", " : "")
                + (pathAlias != null ? "pathAlias=" + pathAlias + ", " : "")
                + (getAssetUrl() != null ? "getAssetUrl()=" + getAssetUrl() : "") + "]";
    }
}
