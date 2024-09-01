package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import static java.util.Optional.ofNullable;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class DvidsCreditDeserializer extends StdDeserializer<List<DvidsCredit>> {

    private static final long serialVersionUID = 1L;

    public DvidsCreditDeserializer() {
        this(null);
    }

    public DvidsCreditDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public List<DvidsCredit> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        List<DvidsCredit> credits = new ArrayList<>();
        JsonNode node = p.getCodec().readTree(p);
        if (node instanceof ArrayNode array) {
            for (int i = 0; i < array.size(); i++) {
                JsonNode elem = array.get(i);
                if (elem instanceof ObjectNode object) {
                    credits.add(new DvidsCredit(
                    ofNullable(object.get("id")).map(JsonNode::asInt).orElse(null),
                    ofNullable(object.get("name")).map(JsonNode::asText).orElse(null),
                    ofNullable(object.get("rank")).map(JsonNode::asText).orElse(null),
                    ofNullable(object.get("url")).map(x -> newURL(x.asText())).orElse(null)));
                } else {
                    credits.add(new DvidsCredit(null, null, null, null));
                }
            }
        } else if (node instanceof TextNode text) {
            credits.add(new DvidsCredit(0, text.asText(), null, null));
        }
        return credits;
    }
}
