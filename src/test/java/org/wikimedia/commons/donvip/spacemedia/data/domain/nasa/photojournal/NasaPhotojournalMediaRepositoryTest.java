package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.photojournal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.wikimedia.commons.donvip.spacemedia.data.domain.TestDataJpa;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

@EntityScan(basePackageClasses = { Media.class, NasaPhotojournalMedia.class })
@EnableJpaRepositories(basePackageClasses = { MediaRepository.class, NasaPhotojournalMediaRepository.class })
@ContextConfiguration(classes = TestDataJpa.Config.class)
class NasaPhotojournalMediaRepositoryTest extends TestDataJpa {

    @Autowired
    private NasaPhotojournalMediaRepository mediaRepository;

    @Test
    void injectedRepositoryIsNotNull() {
        assertNotNull(mediaRepository);
    }
}
