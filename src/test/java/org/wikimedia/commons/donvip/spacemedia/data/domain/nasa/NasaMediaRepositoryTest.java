package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa;

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
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.aster.NasaAsterMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.photojournal.NasaPhotojournalMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo.NasaSdoMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sirs.NasaSirsImageRepository;

@EntityScan(basePackageClasses = { Media.class, NasaMedia.class })
@EnableJpaRepositories(basePackageClasses = { MediaRepository.class, NasaMediaRepository.class })
@ContextConfiguration(classes = TestDataJpa.Config.class)
class NasaMediaRepositoryTest extends TestDataJpa {

    @Autowired
    private List<NasaMediaRepository<?>> mediaRepositories;

    @Autowired
    private NasaAsterMediaRepository asterRepository;

    @Autowired
    private NasaSirsImageRepository sirsRepository;

    @Autowired
    private NasaPhotojournalMediaRepository photojournalRepository;

    @Autowired
    private NasaSdoMediaRepository sdoRepository;

    @Test
    void injectedRepositoriesAreNotNull() {
        assertNotNull(mediaRepositories);
        assertEquals(4, mediaRepositories.size());
        assertNotNull(asterRepository);
        assertNotNull(sirsRepository);
        assertNotNull(photojournalRepository);
        assertNotNull(sdoRepository);
    }
}
