package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class ApiAssetResponseTest {

    @Test
    void testJsonDeserialisation() throws Exception {
        assertNotNull(new ObjectMapper().registerModules(new Jdk8Module(), new JavaTimeModule())
                .readValue(new File("src/test/resources/dvids/asset-response.json"), ApiAssetResponse.class));
    }
}
