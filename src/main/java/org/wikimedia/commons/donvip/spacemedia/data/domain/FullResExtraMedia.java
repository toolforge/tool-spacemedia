package org.wikimedia.commons.donvip.spacemedia.data.domain;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.time.temporal.Temporal;
import java.util.HashSet;
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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Media that can have an optional extr variant of the main media and full-res.
 *
 * @param <ID> the identifier type
 * @param <D>  the media date type
 */
@MappedSuperclass
public abstract class FullResExtraMedia<ID, D extends Temporal> extends FullResMedia<ID, D> {

    @Embedded
    @AttributeOverrides(value = { @AttributeOverride(column = @Column(name = "extra_asset_url"), name = "assetUrl"),
            @AttributeOverride(column = @Column(name = "extra_readable_image"), name = "readableImage"),
            @AttributeOverride(column = @Column(name = "extra_size"), name = "size"),
            @AttributeOverride(column = @Column(name = "extra_sha1", length = 42), name = "sha1"),
            @AttributeOverride(column = @Column(name = "extra_phash", columnDefinition = "VARCHAR(52)", length = 52), name = "phash") })
    @JsonProperty("extra_metadata")
    protected Metadata extraMetadata = new Metadata();

    @ElementCollection(fetch = FetchType.EAGER)
    @JsonProperty("extra_commons_file_names")
    protected Set<String> extraCommonsFileNames = new HashSet<>();

    @Override
    @PostLoad
    protected void initData() {
        super.initData();
        if (extraMetadata == null) {
            extraMetadata = new Metadata();
        }
    }

    public Metadata getExtraMetadata() {
        return extraMetadata;
    }

    public void setExtraMetadata(Metadata extraMetadata) {
        this.extraMetadata = extraMetadata;
    }

    public Set<String> getExtraCommonsFileNames() {
        return extraCommonsFileNames;
    }

    public void setExtraCommonsFileNames(Set<String> extraCommonsFileNames) {
        this.extraCommonsFileNames = extraCommonsFileNames;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(extraCommonsFileNames, extraMetadata);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        FullResExtraMedia<?, ?> other = (FullResExtraMedia<?, ?>) obj;
        return Objects.equals(extraCommonsFileNames, other.extraCommonsFileNames)
                && Objects.equals(extraMetadata, other.extraMetadata);
    }

    @Override
    public List<String> getAssetsToUpload() {
        List<String> result = super.getAssetsToUpload();
        String extraSha1 = extraMetadata.getSha1();
        if (isNotBlank(extraSha1) && isEmpty(getExtraCommonsFileNames())) {
            result.add(extraSha1);
        }
        return result;
    }

    @Override
    public Set<String> getAllCommonsFileNames() {
        Set<String> result = new LinkedHashSet<>(getCommonsFileNames());
        Optional.ofNullable(getExtraCommonsFileNames()).ifPresent(result::addAll);
        return result;
    }
}
