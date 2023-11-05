package org.wikimedia.commons.donvip.spacemedia.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UtilsTest {

    @Test
    void testUriExists() {
        assertFalse(Utils.uriExists(
                "https://www.nasa.gov/image-detail/gemini_6_7_borman_schirra_goodwill_tour_manilla_presser_march_1966"));
        assertTrue(Utils.uriExists("https://www.nasa.gov"));
    }
}
