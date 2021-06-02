package org.wikimedia.commons.donvip.spacemedia.data.jpa;

import java.net.URL;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface MediaPublicationRepository extends CrudRepository<MediaPublication, PublicationKey> {

    Optional<MediaPublication> findByUrl(URL imageUrl);

}
