package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.lroc;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.wikimedia.commons.donvip.spacemedia.data.domain.TestDataJpa;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

@EntityScan(basePackageClasses = { Media.class, NasaLrocMedia.class })
@EnableJpaRepositories(basePackageClasses = { MediaRepository.class, NasaLrocMediaRepository.class })
class NasaLrocMediaRepositoryTest extends TestDataJpa {

    @Autowired
    private NasaLrocMediaRepository mediaRepository;

    @Test
    void injectedRepositoryIsNotNull() {
        checkInjectedComponentsAreNotNull();
        assertNotNull(mediaRepository);
    }
}
