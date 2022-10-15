package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class DvidsThumbnailDeserializer extends StdDeserializer<URL> {

    private static final long serialVersionUID = 1L;

    public DvidsThumbnailDeserializer() {
        this(null);
    }

    public DvidsThumbnailDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public URL deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (node instanceof ObjectNode object) {
            JsonNode urlNode = object.get("url");
            if (urlNode != null) {
                return new URL(urlNode.asText());
            }
        } else if (node instanceof TextNode text) {
            return new URL(text.asText());
        }
        throw new IllegalArgumentException(Objects.toString(node));
    }
}
