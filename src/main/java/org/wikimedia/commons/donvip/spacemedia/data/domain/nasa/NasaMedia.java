package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(indexes = { @Index(columnList = "sha1, phash, center") })
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "media_type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = NasaAudio.class, name = "audio"),
    @JsonSubTypes.Type(value = NasaImage.class, name = "image"),
    @JsonSubTypes.Type(value = NasaVideo.class, name = "video") }
)
public abstract class NasaMedia extends Media<String, ZonedDateTime> {

    @Id
    @Column(name = "nasa_id", nullable = false, length = 170)
    @JsonProperty("nasa_id")
    private String id;

    @Column(length = 20)
    private String center;

    @JsonProperty("date_created")
    @Column(name = "date_created")
    private ZonedDateTime date;

    @JsonProperty("media_type")
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

    @Override
    public ZonedDateTime getDate() {
        return date;
    }

    @Override
    public void setDate(ZonedDateTime date) {
        this.date = date;
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

    @Transient
    @JsonProperty("asset_url")
    public URL getAssetUrl() {
        return metadata.getAssetUrl();
    }

    @Transient
    @JsonProperty("asset_url")
    public void setAssetUrl(URL assetUrl) {
        metadata.setAssetUrl(assetUrl);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode()
                + Objects.hash(center, date, description, keywords, mediaType, id, title);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        NasaMedia other = (NasaMedia) obj;
        return Objects.equals(center, other.center)
                && Objects.equals(date, other.date) && Objects.equals(description, other.description)
                && Objects.equals(keywords, other.keywords) && mediaType == other.mediaType
                && Objects.equals(id, other.id) && Objects.equals(title, other.title);
    }

    @Override
    public String toString() {
        return "NasaMedia [" + (id != null ? "nasaId=" + id + ", " : "")
                + (title != null ? "title=" + title + ", " : "") + (center != null ? "center=" + center + ", " : "")
                + (date != null ? "date=" + date + ", " : "")
                + (mediaType != null ? "mediaType=" + mediaType + ", " : "")
                + (getAssetUrl() != null ? "assetUrl=" + getAssetUrl() + ", " : "")
                + (metadata != null ? "metadata=" + metadata : "") + "]";
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
}
