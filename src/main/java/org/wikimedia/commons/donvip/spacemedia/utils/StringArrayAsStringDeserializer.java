package org.wikimedia.commons.donvip.spacemedia.utils;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class StringArrayAsStringDeserializer extends StdDeserializer<String> {

    public StringArrayAsStringDeserializer() {
        this(null);
    }

    public StringArrayAsStringDeserializer(Class<?> vc) {
        super(vc);
    }

    private static final long serialVersionUID = 1L;

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        JsonNode node = p.getCodec().readTree(p);
        if (node instanceof ArrayNode array) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array.size(); i++) {
                if (i > 0) {
                    sb.append(' ');
                }
                sb.append(array.get(i).asText());
            }
            return sb.toString();
        }
        return node.asText();
    }
}
