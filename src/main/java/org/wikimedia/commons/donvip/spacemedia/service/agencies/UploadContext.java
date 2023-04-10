package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.util.Set;

import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;

public final class UploadContext<T> {
    private final T media;
    private final Metadata metadata;
    private final Set<String> commonsFilenames;
    private final boolean isManual;

    public UploadContext(T media, Metadata metadata, Set<String> commonsFilenames, boolean isManual) {
        this.media = media;
        this.metadata = metadata;
        this.commonsFilenames = commonsFilenames;
        this.isManual = isManual;
    }

    public T getMedia() {
        return media;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Set<String> getCommonsFilenames() {
        return commonsFilenames;
    }

    public boolean isManual() {
        return isManual;
    }
}