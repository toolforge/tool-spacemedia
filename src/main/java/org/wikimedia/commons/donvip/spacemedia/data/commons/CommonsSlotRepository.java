package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface CommonsSlotRepository extends CrudRepository<CommonsSlot, CommonsSlotId> {

    default Optional<CommonsSlot> findMainByRevision(CommonsRevision revision) {
        return findById(new CommonsSlotId(revision.getId(), 1));
    }

    default Optional<CommonsSlot> findMediaInfoByRevision(CommonsRevision revision) {
        return findById(new CommonsSlotId(revision.getId(), 2));
    }
}
