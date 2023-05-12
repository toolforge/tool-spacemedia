package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class ExifMetadataTest {

    @Test
    void testJsonDeserialisation() throws Exception {
        assertNotNull(new ObjectMapper().readValue(new File("src/test/resources/nasa/P22-003-22-metadata.json"),
                ExifMetadata.class));
    }
}
