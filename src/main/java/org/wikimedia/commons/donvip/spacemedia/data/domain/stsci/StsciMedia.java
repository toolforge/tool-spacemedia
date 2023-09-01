package org.wikimedia.commons.donvip.spacemedia.data.domain.stsci;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.Transient;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithKeywords;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Indexed
public class StsciMedia extends Media implements WithKeywords {

    private static final Pattern HORRIBLE_ID_FORMAT = Pattern.compile("\\d{4}-\\d{3}-[A-Z0-9]{26}");

    @Column(length = 9)
    private String newsId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String objectName;

    @Column(length = 100)
    private String constellation;

    /**
     * Image's credits and acknowledgments.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String credits;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> keywords = new HashSet<>();

    public String getNewsId() {
        return newsId;
    }

    public void setNewsId(String newsId) {
        this.newsId = newsId;
    }

    public String getCredits() {
        return credits;
    }

    public void setCredits(String credits) {
        this.credits = credits;
    }

    @Transient
    @JsonIgnore
    public String getMission() {
        return getId().getRepoId();
    }

    @Override
    public Set<String> getKeywords() {
        return keywords;
    }

    @Override
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
        return 31 * super.hashCode() + Objects.hash(credits, newsId, keywords, objectName, constellation);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        StsciMedia other = (StsciMedia) obj;
        return Objects.equals(credits, other.credits)
                && Objects.equals(newsId, other.newsId) && Objects.equals(keywords, other.keywords)
                && Objects.equals(objectName, other.objectName) && Objects.equals(constellation, other.constellation);
    }

    @Override
    public String toString() {
        return "StsciMedia [id=" + getId() + ", newsId=" + newsId + ", objectName=" + objectName
                + ", constellation=" + constellation + ']';
    }

    @Override
    public boolean isAudio() {
        return false;
    }

    @Override
    public boolean isImage() {
        return getMetadataStream().map(FileMetadata::getAssetUrl)
                .anyMatch(url -> url != null && !url.toExternalForm().toLowerCase(Locale.ENGLISH).endsWith(".pdf"));
    }

    @Override
    public boolean isVideo() {
        return false;
    }

    @Override
    protected String getUploadId(FileMetadata fileMetadata) {
        String uid = super.getUploadId(fileMetadata).replace("-Image", "");
        return HORRIBLE_ID_FORMAT.matcher(uid).matches() ? uid.substring(0, uid.lastIndexOf('-')) : uid;
    }

    public StsciMedia copyDataFrom(StsciMedia mediaFromApi) {
        super.copyDataFrom(mediaFromApi);
        this.credits = mediaFromApi.credits;
        this.keywords = mediaFromApi.keywords;
        this.newsId = mediaFromApi.newsId;
        this.objectName = mediaFromApi.objectName;
        this.constellation = mediaFromApi.constellation;
        return this;
    }
}
