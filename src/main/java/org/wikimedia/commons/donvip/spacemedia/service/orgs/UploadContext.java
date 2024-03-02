package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.wikimedia.commons.donvip.spacemedia.data.domain.UploadMode;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;

public final class UploadContext<T extends Media> {
    private final T media;
    private final FileMetadata metadata;
    private final UploadMode uploadMode;
    private final int minYearUploadAuto;
    private final Predicate<FileMetadata> isPermittedFileType;
    private final boolean isManual;

    public UploadContext(T media, FileMetadata metadata, UploadMode uploadMode, int minYearUploadAuto,
            Predicate<FileMetadata> isPermittedFileType, boolean isManual) {
        this.media = requireNonNull(media);
        this.metadata = requireNonNull(metadata);
        this.uploadMode = requireNonNull(uploadMode);
        this.minYearUploadAuto = minYearUploadAuto;
        this.isPermittedFileType = requireNonNull(isPermittedFileType);
        this.isManual = isManual;
    }

    public boolean shouldUpload() {
        return (uploadMode == UploadMode.AUTO
                || (uploadMode == UploadMode.AUTO_FROM_DATE
                        && (isManual || media.getYear().getValue() >= minYearUploadAuto))
                || uploadMode == UploadMode.MANUAL) && !isForbiddenUpload(metadata, isManual)
                && isEmpty(metadata.getCommonsFileNames()) && isPermittedFileType.test(metadata);
    }

    public boolean shouldUploadAuto() {
        return (uploadMode == UploadMode.AUTO || (uploadMode == UploadMode.AUTO_FROM_DATE
                && (isManual || media.getYear().getValue() >= minYearUploadAuto)))
                && !Boolean.TRUE.equals(metadata.isIgnored()) && isEmpty(metadata.getCommonsFileNames())
                && isPermittedFileType.test(metadata);
    }

    public static boolean isForbiddenUpload(Media media, boolean isManual) {
        return media.getMetadataStream().allMatch(fm -> isForbiddenUpload(fm, isManual));
    }

    public static boolean isForbiddenUpload(FileMetadata fm, boolean isManual) {
        return Boolean.TRUE.equals(fm.isIgnored()) && (!isManual || StringUtils.isBlank(fm.getIgnoredReason())
                || !(fm.getIgnoredReason().contains("block list")
                        || fm.getIgnoredReason().contains("likely")
                        || fm.getIgnoredReason().contains("Photoset ignored")
                        || fm.getIgnoredReason().contains("Public Domain Mark")
                        || fm.getIgnoredReason().contains("Integer.MAX_VALUE")));
    }
}
