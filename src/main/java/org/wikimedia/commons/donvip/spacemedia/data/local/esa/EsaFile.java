package org.wikimedia.commons.donvip.spacemedia.data.local.esa;

import java.net.URL;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

import org.springframework.util.CollectionUtils;
import org.wikimedia.commons.donvip.spacemedia.data.local.Media;

@Entity
public class EsaFile extends Media {

    @Id
    @GeneratedValue
    private Integer id;

    @Column(nullable = false, length = 350)
    private URL url;

    public EsaFile() {
    }

    public EsaFile(@NotNull URL url) {
        this.url = url;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public URL getUrl() {
        return url;
    }
    
    public void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "EsaFile [" + (url != null ? "url=" + url + ", " : "") + (sha1 != null ? "sha1=" + sha1 + ", " : "")
                + (!CollectionUtils.isEmpty(commonsFileNames) ? "commonsFileNames=" + commonsFileNames : "") + "]";
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(id, url);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        EsaFile other = (EsaFile) obj;
        return Objects.equals(id, other.id) && Objects.equals(url, other.url);
    }
}
