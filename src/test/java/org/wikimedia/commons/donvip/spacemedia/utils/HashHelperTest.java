package org.wikimedia.commons.donvip.spacemedia.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HashHelperTest {

    @Test
    void testSimilarityScore() {
        // Diff between
        // https://www.esa.int/ESA_Multimedia/Images/2022/07/Budapest_Hungary
        // and https://www.esa.int/ESA_Multimedia/Images/2021/10/Budapest_Hungary
        // TIFF identical but not JPEG
        assertEquals(0.0, HashHelper.similarityScore("5t5fgitouqj9ia8ujwp9ujox8tap6v4yyoq9tyfyvs4gv8umbu",
                "5t5fgitouqj9ia8ujwp9ujox8tap6v4yyoq9tyfyvs4gv8umbu"));
        assertEquals(0.0546875, HashHelper.similarityScore("5t5fgitouqj9i6k3d4fdtg8p0926o8qs9v73d901tqihkca7ii",
                "5t5fgubml5r39p04i7ywi0x726244n4iai65cxkjao6hfh86mi"));
    }
}
