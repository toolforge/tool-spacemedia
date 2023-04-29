package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;

public final class UploadContext<T> {
    private final T media;
    private final Metadata metadata;
    private final boolean isManual;

    public UploadContext(T media, Metadata metadata, boolean isManual) {
        this.media = media;
        this.metadata = metadata;
        this.isManual = isManual;
    }

    public T getMedia() {
        return media;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public boolean isManual() {
        return isManual;
    }
}