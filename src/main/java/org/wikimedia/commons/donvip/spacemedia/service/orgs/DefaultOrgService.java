package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.DefaultMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public abstract class DefaultOrgService<T extends DefaultMedia> extends AbstractOrgService<T, CompositeMediaId> {

    protected DefaultOrgService(MediaRepository<T, CompositeMediaId> repository, String id) {
        super(repository, id);
    }

    @Override
    protected final CompositeMediaId getMediaId(String id) {
        return new CompositeMediaId(id);
    }
}
