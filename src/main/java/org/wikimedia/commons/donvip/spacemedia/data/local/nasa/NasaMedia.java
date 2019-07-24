package org.wikimedia.commons.donvip.spacemedia.data.local.nasa;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "media_type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = NasaAudio.class, name = "audio"),
    @JsonSubTypes.Type(value = NasaImage.class, name = "image"),
    @JsonSubTypes.Type(value = NasaVideo.class, name = "video") }
)
public abstract class NasaMedia {

    @Id
    @NotNull
    @JsonProperty("nasa_id")
    private String nasaId;
    @Column(length = 860)
    private String title;
    @Column(length = 20)
    private String center;
    @JsonProperty("date_created")
    private ZonedDateTime dateCreated;
    @Lob
    private String description;
    @JsonProperty("media_type")
    private NasaMediaType mediaType;
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> keywords;
    @JsonProperty("asset_url")
    private URL assetUrl;
    @Column(nullable = false, length = 42)
    private String sha1;
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> commonsFileNames;

    public String getNasaId() {
        return nasaId;
    }
    public void setNasaId(String nasaId) {
        this.nasaId = nasaId;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getCenter() {
        return center;
    }
    public void setCenter(String center) {
        this.center = center;
    }
    public ZonedDateTime getDateCreated() {
        return dateCreated;
    }
    public void setDateCreated(ZonedDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Set<String> getKeywords() {
        return keywords;
    }
    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }
    public NasaMediaType getMediaType() {
        return mediaType;
    }
    public void setMediaType(NasaMediaType mediaType) {
        this.mediaType = mediaType;
    }
    public URL getAssetUrl() {
        return assetUrl;
    }
    public void setAssetUrl(URL assetUrl) {
        this.assetUrl = assetUrl;
    }
    public String getSha1() {
        return sha1;
    }
    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }
    public Set<String> getCommonsFileNames() {
        return commonsFileNames;
    }
    public void setCommonsFileNames(Set<String> commonsFileNames) {
        this.commonsFileNames = commonsFileNames;
    }
}
