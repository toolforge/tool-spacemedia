package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.time.Year;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikidata.wdtk.datamodel.implementation.ItemIdValueImpl;
import org.wikidata.wdtk.datamodel.implementation.PropertyIdValueImpl;
import org.wikidata.wdtk.datamodel.implementation.StatementGroupImpl;
import org.wikidata.wdtk.datamodel.implementation.StatementImpl;
import org.wikidata.wdtk.datamodel.implementation.ValueSnakImpl;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaAudioRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaImage;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaImageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaVideoRepository;
import org.wikimedia.commons.donvip.spacemedia.service.nasa.NasaMediaProcessorService;

@SpringJUnitConfig(NasaLibraryServiceTest.TestConfig.class)
@TestPropertySource("/application-test.properties")
class NasaLibraryServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private NasaMediaRepository<NasaMedia> repository;

    @MockBean
    private NasaAudioRepository audioRepository;

    @MockBean
    private NasaImageRepository imageRepository;

    @MockBean
    private NasaVideoRepository videoRepository;

    @MockBean
    private NasaMediaProcessorService processor;

    @Autowired
    private NasaLibraryService service;

    @Autowired
    private Environment env;

    @Test
    void testIssPattern() {
        Matcher m = NasaLibraryService.ISS_PATTERN.matcher("iss068e029662");
        assertTrue(m.matches());
        assertEquals("68", m.group(1));
    }

    @Test
    void testIssCategories() throws IOException {
        NasaMedia media = new NasaImage();
        media.setPublicationYear(Year.of(2022));
        media.setId(new CompositeMediaId("", "iss068e031231"));
        media.setDescription(
                "iss068e031231 (Dec. 20, 2022) --- Expedition 68 Flight Engineer Anna Kikina and International Space Station Commander Sergey Prokopyev, both from Roscosmos, are pictured working together inside the Zvezda service module.");
        ItemIdValueImpl s = new ItemIdValueImpl("Q112110246", "wikidata");
        PropertyIdValueImpl p = new PropertyIdValueImpl("P1029", "wikidata");
        when(wikidata.findCommonsStatementGroup("Category:ISS Expedition 68", "P1029"))
                .thenReturn(Optional.of(new StatementGroupImpl(
                        List.of(s(s, p, "Q21154027"), s(s, p, "Q50379760"), s(s, p, "Q30315159"), s(s, p, "Q379039"),
                                s(s, p, "Q3197699"), s(s, p, "Q30611798"), s(s, p, "Q30202712"), s(s, p, "Q13500573"),
                                s(s, p, "Q13484630"), s(s, p, "Q375566"), s(s, p, "Q18352451")))));
        when(wikidata.mapCommonsCategoriesByFamilyName(any())).thenReturn(
                Map.ofEntries(e("Mann", "Nicole Mann"), e("Cassada", "Josh A. Cassada"), e("Hines", "Robert Hines"),
                        e("Wakata", "Koichi Wakata"), e("Watkins", "Jessica Watkins"), e("Petelin", "Dmitri Petelin"),
                        e("Prokopyev", "Sergey Prokopyev (cosmonaut)"), e("Cristoforetti", "Samantha Cristoforetti"),
                        e("Lindgren", "Kjell Lindgren"), e("Kikina", "Anna Kikina"), e("Rubio", "Francisco Rubio")));
        assertEquals(Set.of("ISS Expedition 68", "Anna Kikina", "Sergey Prokopyev (cosmonaut)"),
                service.findCategories(media, new FileMetadata(new URL("https://foo")), false));
    }

    @Test
    void testUploadFileName() {
        NasaMedia media = new NasaImage();
        media.setId(new CompositeMediaId("", "GSFC_20220415_PACE_036720"));
        media.setTitle(
                "OCI Installed to Ground Support Equipment Application for Tilt or RotationThe Ocean Color Instrument (OCI) is installed onto the Ground Support Equipment Application for Tilt or Rotation (GAToR) made by Newton Engineering in a black out tent cleanroom. GAToR will allow engineers to tilt and rotate OCI in different orientations for further testing prior to integration onto the PACE (Plankton, Aerosol, Cloud, ocean Ecosystem) spacecraft.");

        String uploadTitle = media.getUploadTitle(null) + ".jpeg";
        assertTrue(uploadTitle.length() < 240);
        assertEquals(
                "OCI Installed to Ground Support Equipment Application for Tilt or RotationThe Ocean Color Instrument (OCI) is installed onto the Ground Support Equipment Application for Tilt or Rotation (GAToR) made by New (GSFC_20220415_PACE_036720).jpeg",
                uploadTitle);
    }

    @Test
    void testNasaCentersHomePages() {
        Map<String, String> errors = new TreeMap<>();
        for (String center : NasaLibraryService.NASA_CENTERS) {
            URL homePage = env.getProperty("nasa." + center.toLowerCase(Locale.ENGLISH) + ".home.page", URL.class);
            assertNotNull(homePage, env.toString());
            try {
                URLConnection conn = homePage.openConnection();
                if (conn instanceof HttpURLConnection httpConn) {
                    String message = homePage + " -> " + httpConn.getResponseCode();
                    if (httpConn.getResponseCode() != 200 && httpConn.getResponseCode() != 429) {
                        System.err.println(message);
                        errors.put(center, message);
                    } else {
                        System.out.println(message);
                    }
                } else {
                    errors.put(center, homePage + " -> " + conn.toString());
                }
            } catch (IOException e) {
                errors.put(center, homePage + " -> " + e.toString());
            }
        }
        assertTrue(errors.isEmpty(), errors.toString());
    }

    private static Statement s(ItemIdValue subject, PropertyIdValue property, String targetQid) {
        return new StatementImpl(null, new ValueSnakImpl(property, new ItemIdValueImpl(targetQid, "wikidata")),
                subject);
    }

    private static final SimpleEntry<String, String> e(String k, String v) {
        return new SimpleEntry<>(k, v);
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public NasaLibraryService service(NasaMediaRepository<NasaMedia> repository) {
            return new NasaLibraryService(repository);
        }
    }
}
