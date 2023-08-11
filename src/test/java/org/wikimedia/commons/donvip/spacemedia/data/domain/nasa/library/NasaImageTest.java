package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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

    @Test
    void testJsonDeserialization() throws Exception {
        NasaImage img = new ObjectMapper().registerModules(new Jdk8Module(), new JavaTimeModule()).readValue(
                """
                        {
                             "center": "JSC",
                             "date_created": "2022-09-29T00:00:00Z",
                             "description": "Northrop Grumman and subcontractor Thales Alenia Space complete fabrication work on the Habitation and Logistics Outpost (HALO) module, one of two of the Gateway Space Station's habitation elements where astronauts will live and work in lunar orbit during deep space Artemis missions. ",
                             "keywords": [
                               "Gateway",
                               "HALO",
                               "Habitation and Logistics Outpost",
                               "space station",
                               "Artemis",
                               "Moon",
                               "Northrop Grumman",
                               "Moon"
                             ],
                             "location": "Turin, Italy",
                             "media_type": "image",
                             "nasa_id": "3521bfb5-9f9a-43ec-92aa-cb5486d43a88",
                             "secondary_creator": "Northrop Grumman, Thales Alenia Space",
                             "title": "3521bfb5-9f9a-43ec-92aa-cb5486d43a88",
                             "album": [
                               "Gateway"
                             ]
                        }""",
                NasaImage.class);
        assertNotNull(img);
        assertEquals("JSC", img.getCenter());
        assertEquals(
                "Northrop Grumman and subcontractor Thales Alenia Space complete fabrication work on the Habitation and Logistics Outpost (HALO) module, one of two of the Gateway Space Station's habitation elements where astronauts will live and work in lunar orbit during deep space Artemis missions. ",
                img.getDescription());
        assertEquals(Set.of("Gateway", "HALO", "Habitation and Logistics Outpost", "space station", "Artemis", "Moon",
                "Northrop Grumman"), img.getKeywords());
        assertEquals("3521bfb5-9f9a-43ec-92aa-cb5486d43a88", img.getId());
        assertEquals("Northrop Grumman, Thales Alenia Space", img.getSecondaryCreator());
        assertEquals("3521bfb5-9f9a-43ec-92aa-cb5486d43a88", img.getTitle());
    }
}
