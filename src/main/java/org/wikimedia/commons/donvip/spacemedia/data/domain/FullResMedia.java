package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.time.temporal.Temporal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.FetchType;
import javax.persistence.MappedSuperclass;
import javax.persistence.PostLoad;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Media that can have an optional full-res variant of the main media (ex: big TIFF file).
 *
 * @param <ID> the identifier type
 * @param <D>  the media date type
 */
@MappedSuperclass
public abstract class FullResMedia<ID, D extends Temporal> extends Media<ID, D> {

    @Embedded
    @AttributeOverrides(value = {
            @AttributeOverride(column = @Column(name = "full_res_asset_url"), name = "assetUrl"),
            @AttributeOverride(column = @Column(name = "full_res_readable_image"), name = "readableImage"),
            @AttributeOverride(column = @Column(name = "full_res_size"), name = "size"),
            @AttributeOverride(column = @Column(name = "full_res_sha1", length = 42), name = "sha1"),
            @AttributeOverride(column = @Column(name = "full_res_phash", columnDefinition = "VARCHAR(52)", length = 52), name = "phash") })
    protected Metadata fullResMetadata = new Metadata();

    @ElementCollection(fetch = FetchType.EAGER)
    protected Set<String> fullResCommonsFileNames;

    @Override
    @PostLoad
    protected void initData() {
        super.initData();
        if (fullResMetadata == null) {
            fullResMetadata = new Metadata();
        }
    }

    public Metadata getFullResMetadata() {
        return fullResMetadata;
    }

    public void setFullResMetadata(Metadata fullResMetadata) {
        this.fullResMetadata = fullResMetadata;
    }

    public Set<String> getFullResCommonsFileNames() {
        return fullResCommonsFileNames;
    }

    public void setFullResCommonsFileNames(Set<String> fullResCommonsFileNames) {
        this.fullResCommonsFileNames = fullResCommonsFileNames;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(fullResCommonsFileNames, fullResMetadata);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        FullResMedia<?, ?> other = (FullResMedia<?, ?>) obj;
        return Objects.equals(fullResCommonsFileNames, other.fullResCommonsFileNames)
                && Objects.equals(fullResMetadata, other.fullResMetadata);
    }

    @Override
    public List<String> getAssetsToUpload() {
        List<String> result = super.getAssetsToUpload();
        String fullResSha1 = fullResMetadata.getSha1();
        if (StringUtils.isNotBlank(fullResSha1) && CollectionUtils.isEmpty(getFullResCommonsFileNames())) {
            result.add(fullResSha1);
        }
        return result;
    }

    @Override
    public Set<String> getAllCommonsFileNames() {
        Set<String> result = new LinkedHashSet<>(getCommonsFileNames());
        Optional.ofNullable(getFullResCommonsFileNames()).ifPresent(result::addAll);
        return result;
    }
}
