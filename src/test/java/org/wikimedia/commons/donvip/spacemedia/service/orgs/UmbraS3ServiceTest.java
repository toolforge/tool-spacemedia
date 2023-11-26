package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.s3.S3Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.s3.S3MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.GeometryService;
import org.wikimedia.commons.donvip.spacemedia.service.s3.S3Service;

import com.amazonaws.regions.Regions;

@SpringJUnitConfig(UmbraS3ServiceTest.TestConfig.class)
class UmbraS3ServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private S3MediaRepository repository;

    @MockBean
    private GeometryService geometry;

    @MockBean
    private S3Service s3;

    @Autowired
    private UmbraS3Service service;

    @Test
    void testFindCategories() {
        assertEquals(Set.of("Images by Umbra"), service.findCategories(new S3Media(), new FileMetadata()));
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
