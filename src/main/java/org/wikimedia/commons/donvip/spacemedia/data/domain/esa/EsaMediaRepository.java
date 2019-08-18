package org.wikimedia.commons.donvip.spacemedia.data.domain.esa;

import java.net.URL;
import java.util.Optional;

import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMediaRepository;

public interface EsaMediaRepository extends FullResMediaRepository<EsaMedia, Integer> {

    Optional<EsaMedia> findByUrl(URL mediaUrl);
}
