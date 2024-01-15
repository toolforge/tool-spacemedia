package org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class ErccApiTest {

    @Test
    void testGetPagedMapsResponseMapping() throws Exception {
        GetPagedMapsResponse response = readJson("response-1000.json", GetPagedMapsResponse.class);
        assertNotNull(response);
        assertEquals(1000, response.Items().size());
        assertNull(response.LastItemIdentifier());
        assertEquals(4, response.NumberOfPages());
        assertEquals(1, response.PageIndex());
        assertEquals(3068, response.TotalCount());
    }

    @Test
    void testGetPagedMapsResponseMappingNew() throws Exception {
        GetPagedMapsResponse response = readJson("response-new.json", GetPagedMapsResponse.class);
        assertNotNull(response);
        assertEquals(7, response.Items().size());
        assertNull(response.LastItemIdentifier());
        assertEquals(1, response.NumberOfPages());
        assertEquals(1, response.PageIndex());
        assertEquals(7, response.TotalCount());
    }

    @Test
    void testGetMapResponseMapping() throws Exception {
        MapsItem response = readJson("response-map.json", MapsItem.class);
        assertNotNull(response);
        assertNotNull(response.getEchoMapType());
    }

    public static <T> T readJson(String invalidJsonFile, Class<T> klass) throws IOException {
        try (InputStream in = new ByteArrayInputStream(
                Files.readString(Paths.get("src", "test", "resources", "ercc", invalidJsonFile))
                        .replaceAll("\\d,\\d+E\\+\\d+\\.0", "0").getBytes(StandardCharsets.UTF_8))) {
            return new ObjectMapper().registerModules(new Jdk8Module(), new JavaTimeModule()).readValue(in, klass);
        }
    }
}
