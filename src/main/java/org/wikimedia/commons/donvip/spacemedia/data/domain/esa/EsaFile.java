package org.wikimedia.commons.donvip.spacemedia.data.domain.esa;

import java.net.URL;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;

@Entity
@AttributeOverride(name = "title", column = @Column(nullable = true))
@Deprecated
public class EsaFile extends Media {

    @Id
    @GeneratedValue
    private Integer id;

    public EsaFile() {
    }

    public EsaFile(@NotNull URL url) {
        setAssetUrl(url);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "EsaFile [" + (id != null ? "id=" + id + ", " : "") + (sha1 != null ? "sha1=" + sha1 + ", " : "")
                + (assetUrl != null ? "assetUrl=" + assetUrl + ", " : "")
                + (commonsFileNames != null ? "commonsFileNames=" + commonsFileNames + ", " : "")
                + (ignored != null ? "ignored=" + ignored + ", " : "")
                + (ignoredReason != null ? "ignoredReason=" + ignoredReason : "") + "]";
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        EsaFile other = (EsaFile) obj;
        return Objects.equals(id, other.id);
    }
}
