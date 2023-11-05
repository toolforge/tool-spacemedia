package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.jsoup.Jsoup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.webmil.WebMilMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.webmil.WebMilMediaRepository;

@SpringJUnitConfig(classes = AbstractOrgWebMilServiceTest.TestConfig.class)
class AbstractOrgWebMilServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private WebMilMediaRepository repository;

    @Autowired
    private UsSpaceForceWebMilService service;

    @ParameterizedTest
    @CsvSource({ "230829-F-LE393-1171", "191212-F-00000-002", "190923-F-UR189-1004", "221011-F-TD231-1215" })
    void testParseHtml(String virin) throws Exception {
        basicChecks(virin);
    }

    private void basicChecks(String virin) throws IOException {
        WebMilMedia media = new WebMilMedia();
        when(metadataRepository.save(any(FileMetadata.class))).thenAnswer(a -> a.getArgument(0, FileMetadata.class));
        when(commonsService.isPermittedFileUrl(any())).thenReturn(true);

        service.fillMediaWithHtml(null, Jsoup.parse(new File("src/test/resources/webmil/" + virin + ".htm")), media);

        assertNotNull(media.getTitle());
        assertNotNull(media.getDescription());
        assertEquals(virin, media.getVirin());
        assertNotNull(media.getPublicationDate());
        assertNotNull(media.getCreationDate());
        Set<FileMetadata> metadata = media.getMetadata();
        assertEquals(1, metadata.size());
        FileMetadata fm = metadata.iterator().next();
        assertNotNull(fm);
        assertNotNull(fm.getAssetUrl());
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public UsSpaceForceWebMilService service(WebMilMediaRepository repository) {
            return new UsSpaceForceWebMilService(repository, Set.of(
                    "afspc:www.afspc.af.mil/News/Photos/",
                    "buckley:www.buckley.spaceforce.mil/News/Photos/",
                    "jtfsd:www.jtf-spacedefense.mil/News/Photo-and-Video-Gallery/",
                    "losangeles:www.losangeles.spaceforce.mil/News/Photos/",
                    "patrick:www.patrick.spaceforce.mil/News/Photos/",
                    "schriever:www.schriever.spaceforce.mil/News/Photos/",
                    "ssc:www.ssc.spaceforce.mil/Connect-With-Us/Multimedia/",
                    "spacecom:www.spacecom.mil/MEDIA/IMAGERY/",
                    "spaceforce:www.spaceforce.mil/Multimedia/Photos/",
                    "spoc:www.spoc.spaceforce.mil/Multimedia/Photos/",
                    "starcom:www.starcom.spaceforce.mil/News/Photos/",
                    "vandenberg:www.vandenberg.spaceforce.mil/News/Photos/"));
        }
    }
}
