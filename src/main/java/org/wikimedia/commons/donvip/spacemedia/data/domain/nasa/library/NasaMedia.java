package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
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
public abstract class NasaMedia extends SingleFileMedia<String> implements WithKeywords {

    @Id
    @Column(name = "nasa_id", nullable = false, length = 170)
    @JsonProperty("nasa_id")
    private String id;

    @Column(length = 20)
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

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String nasaId) {
        this.id = nasaId;
    }

    public String getCenter() {
        return center;
    }

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
    public int hashCode() {
        return 31 * super.hashCode()
                + Objects.hash(center, location, description, keywords, mediaType, id, title);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        NasaMedia other = (NasaMedia) obj;
        return Objects.equals(center, other.center) && Objects.equals(location, other.location)
                && Objects.equals(description, other.description)
                && Objects.equals(keywords, other.keywords) && mediaType == other.mediaType
                && Objects.equals(id, other.id) && Objects.equals(title, other.title);
    }

    @Override
    public String toString() {
        return "NasaMedia [" + (id != null ? "nasaId=" + id + ", " : "")
                + (title != null ? "title=" + title + ", " : "") + (center != null ? "center=" + center + ", " : "")
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
        setCenter(mediaFromApi.getCenter());
        setLocation(mediaFromApi.getLocation());
        setMediaType(mediaFromApi.getMediaType());
        setKeywords(mediaFromApi.getKeywords());
        return this;
    }
}
