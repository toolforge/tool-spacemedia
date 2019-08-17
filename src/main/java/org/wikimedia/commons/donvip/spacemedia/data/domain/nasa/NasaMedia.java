package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa;

import java.net.URL;
import java.time.ZonedDateTime;
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

import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(indexes = {@Index(columnList = "sha1, center")})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "media_type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = NasaAudio.class, name = "audio"),
    @JsonSubTypes.Type(value = NasaImage.class, name = "image"),
    @JsonSubTypes.Type(value = NasaVideo.class, name = "video") }
)
public abstract class NasaMedia extends Media {

    @Id
    @Column(nullable = false, length = 170)
    @JsonProperty("nasa_id")
    private String nasaId;

    @Column(length = 20)
    private String center;

    @JsonProperty("date_created")
    private ZonedDateTime dateCreated;

    @JsonProperty("media_type")
    private NasaMediaType mediaType;

    @Column(length = 340)
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> keywords;

    public String getNasaId() {
        return nasaId;
    }

    public void setNasaId(String nasaId) {
        this.nasaId = nasaId;
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

    @Override
    @JsonProperty("asset_url")
    public URL getAssetUrl() {
        return super.getAssetUrl();
    }

    @Override
    @JsonProperty("asset_url")
    public void setAssetUrl(URL assetUrl) {
        super.setAssetUrl(assetUrl);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode()
                + Objects.hash(center, dateCreated, description, keywords, mediaType, nasaId, title);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        NasaMedia other = (NasaMedia) obj;
        return Objects.equals(center, other.center)
                && Objects.equals(dateCreated, other.dateCreated) && Objects.equals(description, other.description)
                && Objects.equals(keywords, other.keywords) && mediaType == other.mediaType
                && Objects.equals(nasaId, other.nasaId) && Objects.equals(title, other.title);
    }

    @Override
    public String toString() {
        return "NasaMedia [" + (nasaId != null ? "nasaId=" + nasaId + ", " : "")
                + (title != null ? "title=" + title + ", " : "") + (center != null ? "center=" + center + ", " : "")
                + (dateCreated != null ? "dateCreated=" + dateCreated + ", " : "")
                + (mediaType != null ? "mediaType=" + mediaType + ", " : "")
                + (getAssetUrl() != null ? "assetUrl=" + getAssetUrl() + ", " : "")
                + (sha1 != null ? "sha1=" + sha1 : "") + "]";
    }
}
