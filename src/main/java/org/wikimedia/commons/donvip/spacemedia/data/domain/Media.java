package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.net.URL;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

@MappedSuperclass
@Table(indexes = {@Index(columnList = "sha1")})
public abstract class Media {

    @Column(nullable = false, length = 42)
    protected String sha1;

    @Column(nullable = false, length = 380)
    protected URL assetUrl;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    protected String title;

    @Lob
    @Column(nullable = true, columnDefinition = "TEXT")
    protected String description;

    @ElementCollection(fetch = FetchType.EAGER)
    protected Set<String> commonsFileNames;

    @Column(nullable = true)
    protected Boolean ignored;

    protected String ignoredReason;

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public URL getAssetUrl() {
        return assetUrl;
    }

    public void setAssetUrl(URL assetUrl) {
        this.assetUrl = assetUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getCommonsFileNames() {
        return commonsFileNames;
    }

    public void setCommonsFileNames(Set<String> commonsFileNames) {
        this.commonsFileNames = commonsFileNames;
    }

    public Boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(Boolean ignored) {
        this.ignored = ignored;
    }

    public String getIgnoredReason() {
        return ignoredReason;
    }

    public void setIgnoredReason(String ignoredReason) {
        this.ignoredReason = ignoredReason;
    }

    public Boolean getIgnored() {
        return ignored;
    }

    @Override
    public int hashCode() {
        return Objects.hash(commonsFileNames, sha1, assetUrl);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Media other = (Media) obj;
        return Objects.equals(commonsFileNames, other.commonsFileNames)
            && Objects.equals(sha1, other.sha1)
            && Objects.equals(assetUrl, other.assetUrl);
    }
}
