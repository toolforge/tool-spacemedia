package org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DjangoplicityMediaTest {

    @Test
    void testGetters() {
        DjangoplicityMedia m = new DjangoplicityMedia() {

        };
        assertFalse(m.isAudio());
        assertTrue(m.isImage());
        assertFalse(m.isVideo());
    }
}
