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

    public long countAllMedia() {
        return repository.count();
    }

    public long countMissingMedia() {
        return repository.countMissingInCommons();
    }

    public Iterable<T> listAllMedia() {
        return repository.findAll();
    }

    public List<T> listMissingMedia() {
        return repository.findMissingInCommons();
    }

    public List<T> listDuplicateMedia() {
        return repository.findDuplicateInCommons();
    }

    public abstract List<T> updateMedia() throws IOException;
}
