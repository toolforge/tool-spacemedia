package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.geo.Point;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.photojournal.NasaPhotojournalMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.photojournal.NasaPhotojournalMediaRepository;

@SpringJUnitConfig(NasaPhotojournalServiceTest.TestConfig.class)
class NasaPhotojournalServiceTest extends AbstractAgencyServiceTest {

    @MockBean
    private NasaPhotojournalMediaRepository repository;

    @Autowired
    private NasaPhotojournalService service;

    @Test
    void testGetLocation() {
        NasaPhotojournalMedia media = new NasaPhotojournalMedia();
        media.setDescription(
                "The image was acquired June 17, 2013, covers an area of 9.2 by 10.2 km, and is located at 57.1 degrees north, 7.3 degrees west.");
        assertEquals(new Point(57.1, -7.3), service.getLocation(media).get());
    }

    @Test
    void testGetCreationDate() {
        NasaPhotojournalMedia media = new NasaPhotojournalMedia();
        media.setDescription(
                "The image was acquired June 17, 2013, covers an area of 9.2 by 10.2 km, and is located at 57.1 degrees north, 7.3 degrees west.");
        assertEquals(LocalDate.of(2013, 6, 17), service.getCreationDate(media).get());
    }

    @Configuration
    @Import(DefaultAgencyTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public NasaPhotojournalService service(NasaPhotojournalMediaRepository repository) {
            return new NasaPhotojournalService(repository);
        }
    }
}
