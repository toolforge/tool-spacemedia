package org.wikimedia.commons.donvip.spacemedia.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void testGetFirstSentence() {
        assertEquals("", Utils.getFirstSentence(null));
        assertEquals("", Utils.getFirstSentence(""));
        assertEquals("U.S. Space Force Sgt",
            Utils.getFirstSentence("U.S. Space Force Sgt. Sergkei Triantafyllidis, a technical training instructor with the United States Air Force Honor Guard, looks on as Honor Guard trainees of Class 24D work on their drill movements on Joint Base Anacostia-Bolling, Washington, D.C., July 22, 2024. The class made history on Aug. 16, 2024, as the first joint class of trainees from two military branches to graduate U.S. Air Honor Guard technical training. (U.S. Air Force photo by Robert W. Mitchell)"));
    }
}
