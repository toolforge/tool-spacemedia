package org.wikimedia.commons.donvip.spacemedia.data.local.esa;

import java.net.URL;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

import org.springframework.util.CollectionUtils;

@Entity
public class EsaFile {

    @Id
    @Column(nullable = false, length = 350)
    private URL url;
    @Column(nullable = false, length = 42)
    private String sha1;
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> commonsFileNames;
    @Column(nullable = true)
    private Boolean ignored;
    private String ignoredReason;

    public EsaFile() {
    }

    public EsaFile(@NotNull URL url) {
        this.url = url;
    }

    public URL getUrl() {
        return url;
    }
    
    public void setUrl(URL url) {
        this.url = url;
    }
    
    public String getSha1() {
        return sha1;
    }
    
    public void setSha1(String sha1) {
        this.sha1 = sha1;
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
    public String toString() {
        return "EsaFile [" + (url != null ? "url=" + url + ", " : "") + (sha1 != null ? "sha1=" + sha1 + ", " : "")
                + (!CollectionUtils.isEmpty(commonsFileNames) ? "commonsFileNames=" + commonsFileNames : "") + "]";
    }
}
