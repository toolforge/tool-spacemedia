package org.wikimedia.commons.donvip.spacemedia.data.domain.stsci;

import java.net.URL;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMedia;

@Entity
@Indexed
@Table(indexes = { @Index(columnList = "sha1, full_res_sha1, phash, full_res_phash") })
public class StsciMedia extends FullResMedia<String, ZonedDateTime> {

    private static final Pattern HORRIBLE_ID_FORMAT = Pattern.compile("\\d{4}-\\d{3}-[A-Z0-9]{26}");

    @Id
    @Column(length = 35)
    private String id;

    @Column(length = 9)
    private String newsId;

    @Column(name = "release_date", nullable = false)
    private ZonedDateTime date;

    @Column(name = "exposure_date", nullable = true)
    private LocalDate exposureDate;

    @Column(length = 255)
    private String objectName;

    @Column(length = 255)
    private String constellation;

    /**
     * Image's credits and acknowledgments.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String credits;

    /**
     * Space Telescope or telescope website, the Image belongs to. It is usually
     * 'hubble', 'webb', etc.
     */
    @Column(length = 10)
    private String mission;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> keywords = new HashSet<>();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getNewsId() {
        return newsId;
    }

    public void setNewsId(String newsId) {
        this.newsId = newsId;
    }

    @Override
    public ZonedDateTime getDate() {
        return date;
    }

    @Override
    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    public LocalDate getExposureDate() {
        return exposureDate;
    }

    public void setExposureDate(LocalDate exposureDate) {
        this.exposureDate = exposureDate;
    }

    public String getCredits() {
        return credits;
    }

    public void setCredits(String credits) {
        this.credits = credits;
    }

    public String getMission() {
        return mission;
    }

    public void setMission(String mission) {
        this.mission = mission;
    }

    public Set<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getConstellation() {
        return constellation;
    }

    public void setConstellation(String constellation) {
        this.constellation = constellation;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode()
                + Objects.hash(credits, date, id, mission, newsId, keywords, objectName, constellation);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        StsciMedia other = (StsciMedia) obj;
        return Objects.equals(credits, other.credits) && Objects.equals(date, other.date)
                && Objects.equals(id, other.id) && Objects.equals(mission, other.mission)
                && Objects.equals(newsId, other.newsId) && Objects.equals(keywords, other.keywords)
                && Objects.equals(objectName, other.objectName) && Objects.equals(constellation, other.constellation);
    }

    @Override
    public String toString() {
        return "StsciMedia [id=" + id + ", newsId=" + newsId + ", date=" + date + ", objectName=" + objectName
                + ", constellation=" + constellation + ", mission=" + mission + ']';
    }

    @Override
    public boolean isAudio() {
        return false;
    }

    @Override
    public boolean isImage() {
        URL assertUrl = getMetadata().getAssetUrl();
        return assertUrl != null && !assertUrl.toExternalForm().toLowerCase(Locale.ENGLISH).endsWith(".pdf");
    }

    @Override
    public boolean isVideo() {
        return false;
    }

    @Override
    protected String getUploadId() {
        String id = super.getUploadId().replace("-Image", "");
        return HORRIBLE_ID_FORMAT.matcher(id).matches() ? id.substring(0, id.lastIndexOf('-')) : id;
    }

    public StsciMedia copyDataFrom(StsciMedia mediaFromApi) {
        super.copyDataFrom(mediaFromApi);
        this.credits = mediaFromApi.credits;
        this.date = mediaFromApi.date;
        this.exposureDate = mediaFromApi.exposureDate;
        this.keywords = mediaFromApi.keywords;
        this.mission = mediaFromApi.mission;
        this.newsId = mediaFromApi.newsId;
        this.objectName = mediaFromApi.objectName;
        this.constellation = mediaFromApi.constellation;
        return this;
    }
}
