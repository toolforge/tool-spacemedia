package org.wikimedia.commons.donvip.spacemedia.utils;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class FlexibleDoubleDeserializer extends JsonDeserializer<Double> {

    @Override
    public Double deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String string = parser.getText();
        if (string.contains(",")) {
            string = string.replace(",", ".");
        }
        return Double.valueOf(string);
    }
}
