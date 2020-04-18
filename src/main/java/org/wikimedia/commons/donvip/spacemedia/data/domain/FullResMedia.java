package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.net.URL;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.FetchType;
import javax.persistence.MappedSuperclass;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Media that can have an optional full-res variant of the main media (ex: big
 * TIFF file).
 */
@MappedSuperclass
public abstract class FullResMedia<ID, D extends Temporal> extends Media<ID, D> {

    @Column(nullable = true, name = "full_res_sha1", length = 42)
    protected String fullResSha1;

    @Column(nullable = true, length = 380)
    protected URL fullResAssetUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    protected Set<String> fullResCommonsFileNames;

    public String getFullResSha1() {
        return fullResSha1;
    }

    public void setFullResSha1(String fullResSha1) {
        this.fullResSha1 = fullResSha1;
    }

    public URL getFullResAssetUrl() {
        return fullResAssetUrl;
    }

    public void setFullResAssetUrl(URL fullResAssetUrl) {
        this.fullResAssetUrl = fullResAssetUrl;
    }

    public Set<String> getFullResCommonsFileNames() {
        return fullResCommonsFileNames;
    }

    public void setFullResCommonsFileNames(Set<String> fullResCommonsFileNames) {
        this.fullResCommonsFileNames = fullResCommonsFileNames;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(fullResCommonsFileNames, fullResAssetUrl, fullResSha1);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        FullResMedia<?, ?> other = (FullResMedia<?, ?>) obj;
        return Objects.equals(fullResCommonsFileNames, other.fullResCommonsFileNames)
                && Objects.equals(fullResAssetUrl, other.fullResAssetUrl)
                && Objects.equals(fullResSha1, other.fullResSha1);
    }

    @Override
    public List<String> getAssetsToUpload() {
        List<String> result = super.getAssetsToUpload();
        if (StringUtils.isNotBlank(getFullResSha1()) && CollectionUtils.isEmpty(getFullResCommonsFileNames())) {
            result.add(getFullResSha1());
        }
        return result;
    }
}
