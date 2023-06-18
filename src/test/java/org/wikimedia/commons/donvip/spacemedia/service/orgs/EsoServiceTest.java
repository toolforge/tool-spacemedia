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

@SpringJUnitConfig(EsoServiceTest.TestConfig.class)
class EsoServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private DjangoplicityMediaRepository repository;

    @Autowired
    private EsoService service;

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "eso2215a|Observation|2022-11-10T14:00|1639|1682|Cone Nebula, NGC 2264|Milky Way : Nebula : Type : Star Formation|Nebulae|ESO|https://cdn.eso.org/images/original/eso2215a.tif,https://cdn.eso.org/images/large/eso2215a.jpg|ESO’s 60th anniversary image: the Cone Nebula as seen by the VLT|The Cone Nebula is part of a star-forming region of space, NGC 2264, about 2500 light-years away. Its pillar-like appearance is a perfect example of the shapes that can develop in giant clouds of cold molecular gas and dust, known for creating new stars. This dramatic new view of the nebula was captured with the <a href=\"https://www.eso.org/public/teles-instr/paranal-observatory/vlt/vlt-instr/fors/\">FOcal Reducer and low dispersion Spectrograph 2</a> (FORS2) instrument on ESO’s <a href=\"https://www.eso.org/public/teles-instr/paranal-observatory/vlt/\">Very Large Telescope</a> (VLT), and released on the occasion of ESO’s 60th anniversary.&nbsp;|FORS2, Very Large Telescope" })
    void testReadHtml(String id, DjangoplicityMediaType imageType, String date, int width, int height, String name,
            @ConvertWith(SetArgumentConverter.class) Set<String> types,
            @ConvertWith(SetArgumentConverter.class) Set<String> categories, String credit,
            @ConvertWith(ListArgumentConverter.class) List<String> assetUrls, String title, String description,
            @ConvertWith(SetArgumentConverter.class) Set<String> telescopes)
            throws Exception {
        when(metadataRepository.save(any(FileMetadata.class))).thenAnswer(a -> a.getArgument(0, FileMetadata.class));
        doDjangoplicityMediaTest(service.newMediaFromHtml(html("eso/" + id + ".html"),
                new URL("https://www.eso.org/public/images/" + id + "/"), id, null), id, imageType, date,
                new ImageDimensions(width, height), name, types, categories, credit, assetUrls, title, description,
                telescopes);
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public EsoService service(DjangoplicityMediaRepository repository,
                @Value("${eso.search.link}") String searchLink) {
            return new EsoService(repository, searchLink);
        }
    }
}
