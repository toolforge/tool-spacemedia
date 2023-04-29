package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.time.temporal.Temporal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.PostLoad;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @AssociationOverrides(value = {
            @AssociationOverride(joinColumns = @JoinColumn(name = "extra_commons_file_names"), name = "commonsFileNames"),
            @AssociationOverride(joinColumns = @JoinColumn(name = "extra_exif_id"), name = "exif") })
    @AttributeOverrides(value = {
            @AttributeOverride(column = @Column(name = "extra_asset_url"), name = "assetUrl"),
            @AttributeOverride(column = @Column(name = "extra_readable_image"), name = "readableImage"),
            @AttributeOverride(column = @Column(name = "extra_size"), name = "size"),
            @AttributeOverride(column = @Column(name = "extra_sha1", length = 42), name = "sha1"),
            @AttributeOverride(column = @Column(name = "extra_phash", columnDefinition = "VARCHAR(52)", length = 52), name = "phash") })
    @JsonProperty("extra_metadata")
    protected Metadata extraMetadata = new Metadata();

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

    @Transient
    @JsonIgnore
    public Set<String> getExtraCommonsFileNames() {
        return getExtraMetadata().getCommonsFileNames();
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(extraMetadata);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        FullResExtraMedia<?, ?> other = (FullResExtraMedia<?, ?>) obj;
        return Objects.equals(extraMetadata, other.extraMetadata);
    }

    @Override
    public List<String> getAssetsToUpload() {
        List<String> result = super.getAssetsToUpload();
        if (extraMetadata.shouldUpload()) {
            result.add(extraMetadata.getSha1());
        }
        return result;
    }

    @Override
    public Set<String> getAllCommonsFileNames() {
        Set<String> result = new LinkedHashSet<>(getCommonsFileNames());
        Optional.ofNullable(getExtraCommonsFileNames()).ifPresent(result::addAll);
        return result;
    }

    @Override
    public boolean isAudio() {
        return super.isAudio() || getExtraMetadata().isAudio();
    }

    @Override
    public boolean isImage() {
        return super.isImage() || getExtraMetadata().isImage();
    }

    @Override
    public boolean isVideo() {
        return super.isVideo() || getExtraMetadata().isVideo();
    }

    public FullResExtraMedia<ID, D> copyDataFrom(FullResExtraMedia<ID, D> mediaFromApi) {
        super.copyDataFrom(mediaFromApi);
        if (extraMetadata.getAssetUrl() == null) {
            setExtraMetadata(mediaFromApi.getExtraMetadata());
        }
        return this;
    }
}
