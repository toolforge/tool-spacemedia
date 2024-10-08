package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.s3.S3Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.s3.S3MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.s3.S3Service;

import com.amazonaws.regions.Regions;

@SpringJUnitConfig(UmbraS3ServiceTest.TestConfig.class)
class UmbraS3ServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private S3MediaRepository repository;

    @MockBean
    private S3Service s3;

    @Autowired
    private UmbraS3Service service;

    @Test
    void testFindCategories() throws Exception {
        S3Media media = new S3Media();
        media.setCreationDate(LocalDate.of(2023, 1, 1));
        assertEquals(Set.of("2023 Umbra images"),
                service.findCategories(media, new FileMetadata(new URL("https://foo"))));
    }

    @Test
    void testToIso8601() {
        assertEquals("2023-01-01T01:01:01Z",
                service.toIso8601(ZonedDateTime.of(2023, 1, 1, 1, 1, 1, 123456, ZoneId.of("UTC"))));
    }

    @Test
    void testUploadTitle() {
        S3Media media = new S3Media();
        media.setId(new CompositeMediaId("umbra-open-data-catalog",
                "sar-data/tasks/Komati Power Station, S Africa/4d7c8c0c-7c6f-4252-81e1-6995095d945a/2024-08-21-20-27-04_UMBRA-08/2024-08-21-20-27-04_UMBRA-08_GEC.tif"));
        media.setTitle("Komati Power Station, S Africa (2024-08-21-20-27-04_UMBRA-08)");
        assertEquals("Komati Power Station, S Africa (2024-08-21-20-27-04_UMBRA-08)",
                media.getUploadTitle(new FileMetadata()));
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public UmbraS3Service service(S3MediaRepository repository, @Value("${umbra.s3.region}") Regions region,
                @Value("${umbra.s3.buckets}") Set<String> buckets) {
            return new UmbraS3Service(repository, region, buckets);
        }
    }
}
