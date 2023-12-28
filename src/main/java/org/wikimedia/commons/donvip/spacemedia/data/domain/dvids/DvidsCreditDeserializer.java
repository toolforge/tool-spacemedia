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
                DvidsCredit credit = new DvidsCredit();
                JsonNode elem = array.get(i);
                if (elem instanceof ObjectNode object) {
                    ofNullable(object.get("id")).ifPresent(x -> credit.setId(x.asInt()));
                    ofNullable(object.get("name")).ifPresent(x -> credit.setName(x.asText()));
                    ofNullable(object.get("rank")).ifPresent(x -> credit.setRank(x.asText()));
                    ofNullable(object.get("url")).ifPresent(x -> credit.setUrl(newURL(x.asText())));
                }
                credits.add(credit);
            }
        } else if (node instanceof TextNode text) {
            DvidsCredit credit = new DvidsCredit();
            credit.setId(0);
            credit.setName(text.asText());
            credits.add(credit);
        }
        return credits;
    }
}
