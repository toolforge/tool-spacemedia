package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.net.URL;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface CaptionRepository extends CrudRepository<Caption, Long> {

    Optional<Caption> findByUrl(URL url);

    boolean existsByUrl(URL url);
}
