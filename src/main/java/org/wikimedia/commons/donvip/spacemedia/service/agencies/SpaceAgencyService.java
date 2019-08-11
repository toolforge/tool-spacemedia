package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.wikimedia.commons.donvip.spacemedia.data.local.Media;
import org.wikimedia.commons.donvip.spacemedia.data.local.MediaRepository;

public abstract class SpaceAgencyService<T extends Media, ID> {

    protected final MediaRepository<T, ID> repository;

    public SpaceAgencyService(MediaRepository<T, ID> repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public final long countAllMedia() {
        return repository.count();
    }

    public final long countMissingMedia() {
        return repository.countMissingInCommons();
    }

    public final Iterable<T> listAllMedia() {
        return repository.findAll();
    }

    public final List<T> listMissingMedia() {
        return repository.findMissingInCommons();
    }

    public final List<T> listDuplicateMedia() {
        return repository.findDuplicateInCommons();
    }

    public abstract List<T> updateMedia() throws IOException;
}
