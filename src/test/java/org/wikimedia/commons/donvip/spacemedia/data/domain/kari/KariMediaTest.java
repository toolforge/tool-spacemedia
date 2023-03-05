package org.wikimedia.commons.donvip.spacemedia.data.domain.kari;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class KariMediaTest {

    @Test
    void testGetCreationDateAndMission() {
        KariMedia m = new KariMedia();

        m.setKariId("P_A_Airso_050101_0001");
        assertEquals("Airso", m.getMission());
        assertEquals(LocalDate.of(2005, 1, 1), m.getCreationDate());

        m.setKariId("P_A_EAV-2_121122_0001");
        assertEquals("EAV-2", m.getMission());
        assertEquals(LocalDate.of(2012, 11, 22), m.getCreationDate());

        m.setKariId("P_A_Kachi_930401_0009");
        assertEquals("Kachi", m.getMission());
        assertEquals(LocalDate.of(1993, 4, 1), m.getCreationDate());

        m.setKariId("P_ETC_WORLD ROCKET_151031_0001");
        assertEquals("WORLD ROCKET", m.getMission());
        assertEquals(LocalDate.of(2015, 10, 31), m.getCreationDate());

        m.setKariId("P___180711_0001");
        assertEquals("", m.getMission());
        assertEquals(LocalDate.of(2018, 7, 11), m.getCreationDate());
    }
}
