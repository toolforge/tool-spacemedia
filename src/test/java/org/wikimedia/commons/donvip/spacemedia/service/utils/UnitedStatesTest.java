package org.wikimedia.commons.donvip.spacemedia.service.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates;

class UnitedStatesTest {

    @Test
    void testIsVirin() {
        assertTrue(UnitedStates.isVirin("170831-F-HW403-077"));
    }
}
