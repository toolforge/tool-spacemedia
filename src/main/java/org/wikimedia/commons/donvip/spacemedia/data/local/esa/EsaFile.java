package org.wikimedia.commons.donvip.spacemedia.data.local.esa;

import java.net.URL;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

import org.springframework.util.CollectionUtils;
import org.wikimedia.commons.donvip.spacemedia.data.local.Media;

@Entity
public class EsaFile extends Media {

    @Id
    @Column(nullable = false, length = 350)
    private URL url;
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
