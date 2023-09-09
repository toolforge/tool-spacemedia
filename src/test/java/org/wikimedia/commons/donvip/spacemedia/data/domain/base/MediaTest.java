package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class MediaTest {

    private final Media media = new Media();

    @Test
    void testGetUploadTitle() {
        media.setId(new CompositeMediaId("", "11290168785"));
        media.setTitle("DLR Annual General Assembly / Jahreshauptversammlung &amp; Science Slam 2013");
        assertEquals("DLR Annual General Assembly - Jahreshauptversammlung & Science Slam 2013 (11290168785)",
                media.getUploadTitle(null));

        media.setId(new CompositeMediaId("", "25254739045"));
        media.setTitle(
                "Ein Kolbenkernprobenehmer (piston corer) hängt am Seil neben der Bordwand. Gleich wird er in 1300 mm Tiefe hinabgelassen, um viele Meter in das weiche Sediment auf dem Meeresboden einzudringen und einen entsprechend langen Sedimentkern zu entnehmen.");
        assertEquals(
                "Ein Kolbenkernprobenehmer (piston corer) hängt am Seil neben der Bordwand_ Gleich wird er in 1300 mm Tiefe hinabgelassen, um viele Meter in das weiche Sediment auf dem Meeresboden einzudringen und einen entsprechend lang (25254739045)",
                media.getUploadTitle(null));
        assertEquals(234, media.getUploadTitle(null).length());

        media.setId(new CompositeMediaId("", "NHQ202304210018"));
        media.setTitle("NASA Leadership Meets with UK Space Agency (NHQ202304210018)");
        assertEquals("NASA Leadership Meets with UK Space Agency (NHQ202304210018)", media.getUploadTitle(null));
    }

    @Test
    void testGetSearchTermsInCommons() {
        String id = "PIA25953";
        String description = "<p>NASA's Psyche spacecraft is shown in a clean room on June 26, 2023, at the Astrotech Space Operations facility near the agency's Kennedy Space Center in Florida. Engineers and technicians from the agency's Jet Propulsion Laboratory in Southern California have begun final assembly, test, and launch operations on Psyche. The spacecraft is set to launch atop a SpaceX Falcon Heavy rocket on Oct. 5.</p><p>Measuring about 173 miles (279 kilometers) at its widest point, the asteroid <a href=\"https://solarsystem.nasa.gov/asteroids-comets-and-meteors/asteroids/16-psyche/in-depth/\" target=\"new\">Psyche</a> is a metal-rich body that may be part of a core of a planetesimal, the building block of an early planet. Once the spacecraft reaches Psyche, in the main asteroid belt between Mars and Jupiter, it will spend at least 26 months orbiting the asteroid, gathering images and other data that will tell scientists more about its history and what it is made of.</p><p>Arizona State University leads the Psyche mission. A division of Caltech in Pasadena, JPL is responsible for the mission's overall management, system engineering, integration and test, and mission operations. Maxar Technologies in Palo Alto, California, provided the high-power solar electric propulsion spacecraft chassis.</p><p>JPL also is providing a technology demonstration instrument called <a href=\"https://www.nasa.gov/mission_pages/tdm/dsoc/index.html\" target=\"new\">Deep Space Optical Communications</a> that will fly on Psyche in order to test high-data-rate laser communications that could be used by future NASA missions.</p><p>Psyche is the 14th mission selected as part of <a href=\"https://www.nasa.gov/planetarymissions/discovery.html\" target=\"new\">NASA's Discovery Program</a>, managed by the agency's Marshall Space Flight Center in Huntsville, Alabama.</p><p>For more information about NASA's Psyche mission, go to:<br><a href=\"http://www.nasa.gov/psyche\" target=\"new\">http://www.nasa.gov/psyche</a> or <a href=\"https://psyche.asu.edu/\" target=\"new\">https://psyche.asu.edu/</a></p>";
        media.setId(new CompositeMediaId("", id));
        media.setDescription(description);
        assertEquals(List.of(id, description), media.getSearchTermsInCommons());

        id = "iss066e079006";
        description = "NASA astronaut and Expedition 66 Flight Engineer Raja Chari wears virtual reality goggles inside the International Space Station's Columbus laboratory module. Chari was participating in the GRASP human research experiment that tests how astronauts perceive up and down movements and grip and manipulate objects in microgravity.";
        media.setId(new CompositeMediaId("", id));
        media.setDescription("iss066e079006 (Nov. 22, 2021) --- " + description);
        assertEquals(List.of(id, description), media.getSearchTermsInCommons());
    }

    @Test
    void testIsTitleBlacklisted() {
        assertTrue(Media.isTitleBlacklisted("IMG_5057-CC"));
        assertTrue(Media.isTitleBlacklisted("DSC_1693"));
    }
}
