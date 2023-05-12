package org.wikimedia.commons.donvip.spacemedia.data.domain.noirlab;

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

@EntityScan(basePackageClasses = { Media.class, DjangoplicityMedia.class, NOIRLabMedia.class })
@EnableJpaRepositories(basePackageClasses = { MediaRepository.class, DjangoplicityMediaRepository.class,
        NOIRLabMediaRepository.class })
@ContextConfiguration(classes = TestDataJpa.Config.class)
class NOIRLabMediaRepositoryTest extends TestDataJpa {

    @Autowired
    private NOIRLabMediaRepository mediaRepository;

    @Test
    void injectedRepositoriesAreNotNull() {
        assertNotNull(mediaRepository);
    }
}
