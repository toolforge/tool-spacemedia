package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaType;

@SpringJUnitConfig(IauServiceTest.TestConfig.class)
class IauServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private DjangoplicityMediaRepository repository;

    @Autowired
    private IauService service;

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "ann22001b|Artwork|2022-01-04T11:30|2605|3689||Unspecified|Illustrations|IAU/GA2022|https://www.iau.org/static/archives/images/original/ann22001b.tif,https://www.iau.org/static/archives/images/large/ann22001b.jpg|Season’s Greetings from the IAUGA2022 team|Season’s Greetings from the IAUGA2022 team.|", })
    void testReadHtml(String id, DjangoplicityMediaType imageType, String date, int width, int height, String name,
            @ConvertWith(SetArgumentConverter.class) Set<String> types,
            @ConvertWith(SetArgumentConverter.class) Set<String> categories, String credit,
            @ConvertWith(ListArgumentConverter.class) List<String> assetUrls, String title, String description,
            @ConvertWith(SetArgumentConverter.class) Set<String> telescopes) throws Exception {
        when(metadataRepository.save(any(FileMetadata.class))).thenAnswer(a -> a.getArgument(0, FileMetadata.class));
        doDjangoplicityMediaTest(
                service.newMediaFromHtml(html("iau/" + id + ".html"),
                        new URL("https://www.iau.org/public/images/details/" + id + "/"), id, null),
                id, imageType, date, new ImageDimensions(width, height), name, types, categories, credit, assetUrls,
                title, description, telescopes);
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public IauService service(DjangoplicityMediaRepository repository,
                @Value("${iau.search.link}") String searchLink) {
            return new IauService(repository, searchLink);
        }
    }
}
