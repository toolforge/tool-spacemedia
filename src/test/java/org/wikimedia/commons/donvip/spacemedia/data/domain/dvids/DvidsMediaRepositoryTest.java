package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.wikimedia.commons.donvip.spacemedia.data.domain.TestDataJpa;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

@EntityScan(basePackageClasses = { Media.class, DvidsMedia.class })
@EnableJpaRepositories(basePackageClasses = { MediaRepository.class, DvidsMediaRepository.class })
@ContextConfiguration(classes = TestDataJpa.Config.class)
class DvidsMediaRepositoryTest extends TestDataJpa {

    @Autowired
    private List<DvidsMediaRepository<?>> mediaRepositories;

    @Test
    void injectedRepositoriesAreNotNull() {
        assertNotNull(mediaRepositories);
        assertEquals(8, mediaRepositories.size());
    }
}
