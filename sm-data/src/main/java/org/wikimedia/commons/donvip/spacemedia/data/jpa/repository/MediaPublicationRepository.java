package org.wikimedia.commons.donvip.spacemedia.data.jpa.repository;

import java.net.URL;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.MediaPublication;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.PublicationKey;

public interface MediaPublicationRepository extends CrudRepository<MediaPublication, PublicationKey> {

    default Optional<MediaPublication> findById(String depotId, String id) {
        return findById(new PublicationKey(depotId, id));
    }

    Optional<MediaPublication> findByUrl(URL imageUrl);

    Set<MediaPublication> findByDepotIdIn(Set<String> depotIds);
}
