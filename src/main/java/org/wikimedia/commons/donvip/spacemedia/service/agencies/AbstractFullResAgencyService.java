package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.time.temporal.Temporal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageUploadForbiddenException;

public abstract class AbstractFullResAgencyService<T extends FullResMedia<ID, D>, ID, D extends Temporal, OT extends Media<OID, OD>, OID, OD extends Temporal>
        extends AbstractAgencyService<T, ID, D, OT, OID, OD> {

    private FullResMediaRepository<T, ID, D> fullResRepository;

    public AbstractFullResAgencyService(FullResMediaRepository<T, ID, D> repository, String id) {
        super(repository, id);
        this.fullResRepository = repository;
    }

    @Override
    protected final T findBySha1OrThrow(String sha1) {
        List<T> list = fullResRepository.findByMetadata_Sha1OrFullResMetadata_Sha1(sha1);
        if (CollectionUtils.isEmpty(list)) {
            throw new ImageNotFoundException(sha1);
        }
        return list.get(0);
    }

    @Override
    protected void checkUploadPreconditions(T media) throws IOException {
        super.checkUploadPreconditions(media);
        // Forbid upload of duplicate medias for a single repo, they may have different descriptions
        String fullResSha1 = media.getFullResMetadata().getSha1();
        if (fullResSha1 != null && fullResRepository.countByFullResMetadata_Sha1(fullResSha1) > 1) {
            throw new ImageUploadForbiddenException(media + " is present several times.");
        }
    }

    @Override
    protected void doUpload(String wikiCode, T media) throws IOException {
        super.doUpload(wikiCode, media);
        if (media.getFullResMetadata().getAssetUrl() != null) {
            media.setFullResCommonsFileNames(new HashSet<>(Set.of(
                    commonsService.upload(wikiCode, media.getUploadTitle(),
                    media.getFullResMetadata().getAssetUrl(), media.getFullResMetadata().getSha1()))));
        }
    }
}
