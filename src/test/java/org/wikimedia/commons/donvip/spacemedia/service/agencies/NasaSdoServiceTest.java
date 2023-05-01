package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NasaSdoServiceTest extends AbstractAgencyServiceTest {

    @Test
    void testParseImageDateTime() {
        assertEquals(5, NasaSdoService.parseImageDateTime("20221105_000005_4096_4500.jpg").getSecond());
    }

    @Test
    void testParseMovieDateTime() {
        assertEquals(3, NasaSdoService.parseMovieDateTime("20230403_1024_0094.ogv").getDayOfMonth());
    }
}
