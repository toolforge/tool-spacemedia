package org.wikimedia.commons.donvip.spacemedia.data.jpa.repository;

import java.net.URL;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.MediaPublication;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.PublicationKey;

public interface MediaPublicationRepository extends CrudRepository<MediaPublication, PublicationKey> {

    Optional<MediaPublication> findByUrl(URL imageUrl);

}
