package org.wikimedia.commons.donvip.spacemedia.data.domain.esa;

import java.net.URL;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

import org.springframework.util.CollectionUtils;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;

@Entity
@AttributeOverride(name = "title", column = @Column(nullable = true))
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
        return "EsaFile [" + (getAssetUrl() != null ? "url=" + getAssetUrl() + ", " : "")
                + (sha1 != null ? "sha1=" + sha1 + ", " : "")
                + (!CollectionUtils.isEmpty(commonsFileNames) ? "commonsFileNames=" + commonsFileNames : "") + "]";
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
