package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.time.YearMonth;

import jakarta.persistence.AttributeConverter;

public class YearMonthAttributeConverter implements AttributeConverter<YearMonth, String> {

    @Override
    public String convertToDatabaseColumn(YearMonth attribute) {
        return attribute != null ? attribute.toString() : null;
    }

    @Override
    public YearMonth convertToEntityAttribute(String dbData) {
        return isNotBlank(dbData) ? YearMonth.parse(dbData) : null;
    }
}
