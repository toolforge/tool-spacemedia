package org.wikimedia.commons.donvip.spacemedia.data.domain.eso;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.wikimedia.commons.donvip.spacemedia.data.domain.TestDataJpa;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaRepository;

@EntityScan(basePackageClasses = { Media.class, DjangoplicityMedia.class, EsoMedia.class })
@EnableJpaRepositories(basePackageClasses = { MediaRepository.class, DjangoplicityMediaRepository.class,
        EsoMediaRepository.class })
@ContextConfiguration(classes = TestDataJpa.Config.class)
class EsoMediaRepositoryTest extends TestDataJpa {

    @Autowired
    private EsoMediaRepository mediaRepository;

    @Test
    void injectedRepositoriesAreNotNull() {
        assertNotNull(mediaRepository);
    }
}
