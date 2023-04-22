package org.wikimedia.commons.donvip.spacemedia.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UnitedStatesTest {

    @Test
    void testIsVirin() {
        assertTrue(UnitedStates.isVirin("170831-F-HW403-077"));
    }
}
