package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.SingleFileMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithKeywords;

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
public abstract class NasaMedia extends SingleFileMedia implements WithKeywords {

    @Transient
    @JsonProperty("nasa_id")
    private String nasaId;

    @Transient
    @JsonProperty("center")
    private String center;

    @Column(length = 200, nullable = true)
    private String location;

    @JsonProperty("media_type")
    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false, columnDefinition = "TINYINT default 0")
    private NasaMediaType mediaType;

    @Column(length = 340)
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> keywords = new HashSet<>();

    @JsonProperty("album")
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> albums = new HashSet<>();

    @Transient
    public String getNasaId() {
        return nasaId;
    }

    @Transient
    public void setNasaId(String nasaId) {
        this.nasaId = nasaId;
    }

    @Transient
    public String getCenter() {
        return center;
    }

    @Transient
    public void setCenter(String center) {
        this.center = center;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    @JsonProperty("date_created")
    public ZonedDateTime getCreationDateTime() {
        return super.getCreationDateTime();
    }

    @Override
    @JsonProperty("date_created")
    public void setCreationDateTime(ZonedDateTime creationDateTime) {
        super.setCreationDateTime(creationDateTime);
    }

    @Override
    public Set<String> getKeywords() {
        return keywords;
    }

    @Override
    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }

    public Set<String> getAlbums() {
        return albums;
    }

    public void setAlbums(Set<String> albums) {
        this.albums = albums;
    }

    public NasaMediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(NasaMediaType mediaType) {
        this.mediaType = mediaType;
    }

    @Transient
    @JsonProperty("asset_url")
    public URL getAssetUrl() {
        return getUniqueMetadata().getAssetUrl();
    }

    @Transient
    @JsonProperty("asset_url")
    public void setAssetUrl(URL assetUrl) {
        getUniqueMetadata().setAssetUrl(assetUrl);
    }

    @Override
    public Optional<String> getAlbumName() {
        return getAlbums().stream().findFirst();
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(location, description, keywords, mediaType, title);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        NasaMedia other = (NasaMedia) obj;
        return Objects.equals(location, other.location)
                && Objects.equals(description, other.description)
                && Objects.equals(keywords, other.keywords) && mediaType == other.mediaType
                && Objects.equals(title, other.title);
    }

    @Override
    public String toString() {
        return "NasaMedia [" + (getId() != null ? "id=" + getId() + ", " : "")
                + (title != null ? "title=" + title + ", " : "")
                + (location != null ? "location=" + location + ", " : "")
                + (mediaType != null ? "mediaType=" + mediaType + ", " : "")
                + (getAssetUrl() != null ? "assetUrl=" + getAssetUrl() + ", " : "")
                + (getMetadata() != null ? "metadata=" + getMetadata() : "") + "]";
    }

    @Override
    public final boolean isAudio() {
        return mediaType == NasaMediaType.audio;
    }

    @Override
    public final boolean isImage() {
        return mediaType == NasaMediaType.image;
    }

    @Override
    public final boolean isVideo() {
        return mediaType == NasaMediaType.video;
    }

    public NasaMedia copyDataFrom(NasaMedia mediaFromApi) {
        super.copyDataFrom(mediaFromApi);
        setLocation(mediaFromApi.getLocation());
        setMediaType(mediaFromApi.getMediaType());
        setAlbums(mediaFromApi.getAlbums());
        return this;
    }
}
