package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.net.URL;
import java.time.Year;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Store;

@MappedSuperclass
public abstract class Media<ID, D extends Temporal> {

    @Column(nullable = false, length = 42)
    protected String sha1;

    @Column(nullable = false, length = 380)
    protected URL assetUrl;

    @Column(nullable = true, length = 380)
    protected URL thumbnailUrl;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
    protected String title;

    @Lob
    @Column(nullable = true, columnDefinition = "TEXT")
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
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

    public URL getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(URL thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
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

    public abstract ID getId();

    public abstract void setId(ID id);

    public abstract D getDate();

    public abstract void setDate(D date);

    public Year getYear() {
        return Year.of(getDate().get(ChronoField.YEAR));
    }

    public void setYear(Year photoYear) {
        throw new UnsupportedOperationException();
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
        Media<?, ?> other = (Media<?, ?>) obj;
        return Objects.equals(commonsFileNames, other.commonsFileNames)
            && Objects.equals(sha1, other.sha1)
            && Objects.equals(assetUrl, other.assetUrl);
    }
}
