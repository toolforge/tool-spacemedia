package org.wikimedia.commons.donvip.spacemedia.data.domain.stsci;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithInstruments;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithKeywords;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.Transient;

@Entity
public class StsciMedia extends Media implements WithKeywords, WithInstruments {

    private static final Pattern HORRIBLE_ID_FORMAT = Pattern.compile("\\d{4}-\\d{3}-[A-Z0-9]{26}");

    @Column(length = 9)
    private String newsId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String objectName;

    @Column(length = 100)
    private String constellation;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> keywords = new HashSet<>();

    @Column(nullable = true, length = 63)
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> instruments = new HashSet<>();

    public String getNewsId() {
        return newsId;
    }

    public void setNewsId(String newsId) {
        this.newsId = newsId;
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
    public Set<String> getInstruments() {
        return instruments;
    }

    @Override
    public void setInstruments(Set<String> instruments) {
        this.instruments = instruments;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(newsId, keywords, objectName, constellation, instruments);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        StsciMedia other = (StsciMedia) obj;
        return Objects.equals(newsId, other.newsId) && Objects.equals(keywords, other.keywords)
                && Objects.equals(objectName, other.objectName) && Objects.equals(constellation, other.constellation)
                && Objects.equals(instruments, other.instruments);
    }

    @Override
    public String toString() {
        return "StsciMedia [id=" + getId() + ", newsId=" + newsId + ", objectName=" + objectName
                + ", constellation=" + constellation + ", instruments=" + instruments + ']';
    }

    @Override
    public String getUploadId(FileMetadata fileMetadata) {
        String uid = super.getUploadId(fileMetadata).replace("-Image", "");
        return HORRIBLE_ID_FORMAT.matcher(uid).matches() ? uid.substring(0, uid.lastIndexOf('-')) : uid;
    }

    public StsciMedia copyDataFrom(StsciMedia mediaFromApi) {
        super.copyDataFrom(mediaFromApi);
        this.newsId = mediaFromApi.newsId;
        this.objectName = mediaFromApi.objectName;
        this.constellation = mediaFromApi.constellation;
        this.instruments = mediaFromApi.instruments;
        return this;
    }
}
