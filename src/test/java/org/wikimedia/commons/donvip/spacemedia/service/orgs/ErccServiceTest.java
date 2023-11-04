package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.EchoMapType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.ErccMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.ErccMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.api.MapsItem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@SpringJUnitConfig(ErccServiceTest.TestConfig.class)
class ErccServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private ErccMediaRepository repository;

    @Autowired
    private ErccService service;

    @Test
    void testMapMedia() throws Exception {
        CompositeMediaId id = new CompositeMediaId("Daily", "10");
        ErccMedia media = service.mapMedia(readJson("response-map.json", MapsItem.class), id);
        assertEquals(id, media.getId());
        assertEquals(EchoMapType.Daily, media.getMapType());
        assertNull(media.getSources());
        assertEquals(Set.of("Earthquake"), media.getEventTypes());
        assertNull(media.getContinent());
        assertEquals("Greece", media.getMainCountry());
        assertEquals(Set.of("Greece", "Italy"), media.getCountries());
        assertNull(media.getCategory());
        assertEquals("ECHO Daily Map", media.getDescription());
        assertEquals("ECHO Daily Map", media.getTitle());
        assertEquals(LocalDate.of(2013, 4, 28), media.getPublicationDate());
        assertNull(media.getCreationDate());
        assertEquals(1, media.getMetadata().size());
        assertEquals(new URL(
                "https://erccportal.jrc.ec.europa.eu/API/ERCC/Maps/DownloadPublicMap?contentItemID=10&fileN=ThumbFile&forceDownload=False"),
                media.getThumbnailUrl());
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public ErccService service(ErccMediaRepository repository) {
            return new ErccService(repository);
        }
    }

    public static <T> T readJson(String invalidJsonFile, Class<T> klass) throws IOException {
        try (InputStream in = new ByteArrayInputStream(
                Files.readString(Paths.get("src", "test", "resources", "ercc", invalidJsonFile))
                        .replaceAll("\\d,\\d+E\\+\\d+\\.0", "0").getBytes(StandardCharsets.UTF_8))) {
            return new ObjectMapper().registerModules(new Jdk8Module(), new JavaTimeModule()).readValue(in, klass);
        }
    }
}
