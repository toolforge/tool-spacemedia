package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.hibernate.search.annotations.FieldBridge;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(indexes = { @Index(columnList = "sha1, phash") })
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "id", visible = true)
@JsonTypeIdResolver(value = DvidsMediaTypeIdResolver.class)
public abstract class DvidsMedia extends Media<DvidsMediaTypedId, ZonedDateTime> {

    /**
     * Specific document id to retrieve for search.
     */
    @Id
    @Embedded
    @FieldBridge(impl = DvidsMediaTypedIdFieldBridge.class)
    private DvidsMediaTypedId id;

    /**
     * Date media was acquired by shooter/producer. Date in ISO8601 format.
     */
    @Column(nullable = false)
    private ZonedDateTime date;

    /**
     * Date/time item was published at DVIDS. Date in ISO8601 format.
     */
    @JsonProperty("date_published")
    @Column(name = "date_published", nullable = false)
    private ZonedDateTime datePublished;

    /**
     * Comma separated list of keywords.
     */
    @Column(length = 340)
    @ElementCollection(fetch = FetchType.EAGER)
    @JsonDeserialize(converter = DvidsStringSetConverter.class)
    @JsonSerialize(converter = DvidsSetStringConverter.class)
    private Set<String> keywords = new HashSet<>();

    /**
     * Name of branch that produced this asset.
     */
    private String branch;

    /**
     * DVIDS abbreviation of unit credited with media asset.
     */
    private String unit;

    /**
     * Full name of unit credited with media asset.
     */
    @JsonProperty("unit_name")
    private String unitName;

    /**
     * Who created the asset.
     */
    @ManyToMany(fetch = FetchType.EAGER, cascade = { CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH })
    @JsonDeserialize(using = DvidsCreditDeserializer.class)
    private List<DvidsCredit> credit;

    /**
     * Category of the asset:
     * <ul>
     * <li>One of Combat Operations, Miscellaneous, Afghanistan, Guantanamo, Horn of
     * Africa, Iraq, Kuwait, US, Press Release for news.</li>
     * <li>one of B-Roll, Briefings, Commercials, Greetings, In The Fight,
     * Interviews, Newscasts, Package, PSA, Series for video.</li>
     * <li>one of B-Roll, Briefings, Greetings, Interviews, Newscasts for
     * audio.</li>
     * </ul>
     */
    @Column(nullable = true)
    private String category;

    /**
     * Rating of the asset. Will not be returned if the asset has not been rated.
     */
    @Column(nullable = true)
    private Float rating;

    /**
     * Date in ISO8601 format of when the asset was last updated.
     */
    private ZonedDateTime timestamp;

    /**
     * Info about where the asset was created. Note country_abbreviation will not be
     * present if it can not be determined.
     */
    @Embedded
    private DvidsLocation location;

    /**
     * VIRIN of asset.
     */
    private String virin;

    private static final Pattern ONLY_DIGITS = Pattern.compile("\\d+");

    @Override
    public DvidsMediaTypedId getId() {
        return id;
    }

    @Override
    public void setId(DvidsMediaTypedId id) {
        this.id = id;
    }

    @Override
    public ZonedDateTime getDate() {
        return date;
    }

    @Override
    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    public ZonedDateTime getDatePublished() {
        return datePublished;
    }

    public void setDatePublished(ZonedDateTime datePublished) {
        this.datePublished = datePublished;
    }

    public Set<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }

    @JsonIgnore
    public DvidsMediaType getMediaType() {
        return id.getType();
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public List<DvidsCredit> getCredit() {
        return credit;
    }

    public void setCredit(List<DvidsCredit> credit) {
        this.credit = credit;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Float getRating() {
        return rating;
    }

    public void setRating(Float rating) {
        this.rating = rating;
    }

    @Override
    @JsonProperty("thumbnail")
    public URL getThumbnailUrl() {
        return super.getThumbnailUrl();
    }

    @Override
    @JsonProperty("thumbnail")
    public void setThumbnailUrl(URL thumbnailUrl) {
        super.setThumbnailUrl(thumbnailUrl);
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public DvidsLocation getLocation() {
        return location;
    }

    public void setLocation(DvidsLocation location) {
        this.location = location;
    }

    public String getVirin() {
        return virin;
    }

    public void setVirin(String virin) {
        this.virin = virin;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode()
                + Objects.hash(date, datePublished, description, category, keywords, id, title);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        DvidsMedia other = (DvidsMedia) obj;
        return Objects.equals(date, other.date) && Objects.equals(description, other.description)
                && Objects.equals(branch, other.branch)
                && Objects.equals(datePublished, other.datePublished)
                && Objects.equals(category, other.category)
                && Objects.equals(keywords, other.keywords)
                && Objects.equals(id, other.id) && Objects.equals(title, other.title);
    }

    @Override
    public String toString() {
        return "DvidsMedia [" + (id != null ? "id=" + id + ", " : "")
                + (title != null ? "title=" + title + ", " : "") + (datePublished != null ? "datePublished=" + datePublished + ", " : "")
                + (date != null ? "date=" + date + ", " : "")
                + (category != null ? "category=" + category + ", " : "")
                + (metadata != null ? "metadata=" + metadata : "") + "]";
    }

    @Override
    public final boolean isAudio() {
        return id.getType() == DvidsMediaType.audio;
    }

    @Override
    public final boolean isImage() {
        return id.getType() == DvidsMediaType.image || id.getType() == DvidsMediaType.graphic;
    }

    @Override
    public final boolean isVideo() {
        return id.getType() == DvidsMediaType.video;
    }

    @Override
    public final String getUploadTitle() {
        if (ONLY_DIGITS.matcher(title.replace(" ", "").replace("/", "")).matches()) {
            // Avoid https://commons.wikimedia.org/wiki/MediaWiki:Titleblacklist
            // # File names with no letters, except for some meaningless prefix:
            // File:\P{L}*\.[^.]+
            // File:\P{L}*(small|medium|large)\)?\.[^.]+
            return title + " (" + unit + " " + getId().getId() + ")";
        } else {
            return title + " (" + getId().getId() + ")";
        }
    }

    public DvidsMedia copyDataFrom(DvidsMedia mediaFromApi) {
        setBranch(mediaFromApi.getBranch());
        setDate(mediaFromApi.getDate());
        setDatePublished(mediaFromApi.getDatePublished());
        setDescription(mediaFromApi.getDescription());
        setCategory(mediaFromApi.getCategory());
        setKeywords(mediaFromApi.getKeywords());
        setRating(mediaFromApi.getRating());
        setThumbnailUrl(mediaFromApi.getThumbnailUrl());
        setTimestamp(mediaFromApi.getTimestamp());
        setTitle(mediaFromApi.getTitle());
        setUnit(mediaFromApi.getUnit());
        setUnitName(mediaFromApi.getUnitName());
        setVirin(mediaFromApi.getVirin());
        return this;
    }
}
