package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.time.temporal.Temporal;

import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageUploadForbiddenException;

public abstract class AbstractFullResSpaceAgencyService<T extends FullResMedia<ID, D>, ID, D extends Temporal>
        extends AbstractSpaceAgencyService<T, ID, D> {

    private FullResMediaRepository<T, ID, D> fullResRepository;

    public AbstractFullResSpaceAgencyService(FullResMediaRepository<T, ID, D> repository) {
        super(repository);
        this.fullResRepository = repository;
    }

    @Override
    protected final T findBySha1OrThrow(String sha1) {
        return fullResRepository.findBySha1OrFullResSha1(sha1).orElseThrow(() -> new ImageNotFoundException(sha1));
    }

    @Override
    protected void checkUploadPreconditions(T media) throws IOException {
        super.checkUploadPreconditions(media);
        // Forbid upload of duplicate medias for a single repo, they may have different descriptions
        if (media.getFullResSha1() != null && fullResRepository.countByFullResSha1(media.getFullResSha1()) > 1) {
            throw new ImageUploadForbiddenException(media + " is present several times.");
        }
    }

    @Override
    protected void doUpload(String wikiCode, T media) throws IOException {
        super.doUpload(wikiCode, media);
        if (media.getFullResAssetUrl() != null) {
            commonsService.upload(wikiCode, media.getTitle(), media.getFullResAssetUrl());
        }
    }
}
