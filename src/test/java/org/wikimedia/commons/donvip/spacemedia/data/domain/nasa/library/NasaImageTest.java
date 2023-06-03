package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NasaImageTest {

    @Test
    void testGetUploadTitle() {
        NasaImage img = new NasaImage();
        img.setId("205453_34");
        img.setTitle("205453_34");
        img.setDescription(
                "NASA and Northrop Grumman completed a solid rocket booster motor ground test for future flights of the agency’s Space Launch System rocket at Northrop Grumman’s test facility in Promontory, Utah, July 21. The booster motor, called Flight Support Booster-2 (FSB-2), fired for a little over two minutes and produced more than 3.6 million pounds of thrust. Test data will be used to evaluate improvements and new materials in the boosters for missions after Artemis III. When SLS launches the Artemis missions to the Moon, its two five-segment solid rocket boosters produce more than 75% of the initial thrust. The SLS boosters are the largest, most powerful boosters ever built for flight. For more information about SLS, visit nasa.gov/sls");
        assertEquals(
                "NASA and Northrop Grumman completed a solid rocket booster motor ground test for future flights of the agency’s Space Launch System rocket at Northrop Grumman’s test facility in Promontory, Utah, July 21 (205453_34)",
                img.getUploadTitle(null));
    }
}
