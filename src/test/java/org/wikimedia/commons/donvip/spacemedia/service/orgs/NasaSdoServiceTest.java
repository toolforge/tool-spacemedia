package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo.NasaSdoKeywords;

class NasaSdoServiceTest extends AbstractOrgServiceTest {

    @Test
    void testParseImageDateTime() {
        assertEquals(5, NasaSdoService.parseImageDateTime("20221105_000005_4096_4500.jpg").getSecond());
    }

    @Test
    void testParseMovieDateTime() {
        assertEquals(3, NasaSdoService.parseMovieDateTime("20230403_1024_0094.ogv").getDayOfMonth());
    }

    @ParameterizedTest
    @CsvSource({ "aia_2023.06.02.txt,57599,2023-06-02T00:00:00.580Z",
            "hmi_2023.06.02.txt,46079,2023-06-02T00:00:01.610Z" })
    void testParseKeywords(String filename, int size, String obs) throws IOException {
        try (InputStream in = Files.newInputStream(Paths.get("src", "test", "resources", "nasa", "sdo", filename))) {
            List<NasaSdoKeywords> keywords = NasaSdoService.parseKeywords(in,
                    filename.startsWith("aia") ? NasaSdoService.AIA_DATE_TIME_FORMAT
                            : NasaSdoService.HMI_DATE_TIME_FORMAT);
            assertEquals(size, keywords.size());
            assertEquals(obs, keywords.get(0).gettObs().toString());
        }
    }
}
