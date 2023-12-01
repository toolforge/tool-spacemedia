package org.wikimedia.commons.donvip.spacemedia.data.commons;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CommonsMediaTypeConverter implements AttributeConverter<CommonsMediaType, String> {

    @Override
    public String convertToDatabaseColumn(CommonsMediaType attribute) {
        return attribute.getValue();
    }

    @Override
    public CommonsMediaType convertToEntityAttribute(String dbData) {
        return CommonsMediaType.of(dbData);
    }
}
