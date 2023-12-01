package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;

class YearMonthAttributeConverterTest {

    private final YearMonthAttributeConverter converter = new YearMonthAttributeConverter();

    @Test
    void testConvertToDatabaseColumn() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertEquals("1900-01", converter.convertToDatabaseColumn(YearMonth.of(1900, 1)));
        assertEquals("2023-11", converter.convertToDatabaseColumn(YearMonth.of(2023, 11)));
    }

    @Test
    void testConvertToEntityAttribute() {
        assertNull(converter.convertToEntityAttribute(null));
        assertEquals(YearMonth.of(1900, 1), converter.convertToEntityAttribute("1900-01"));
        assertEquals(YearMonth.of(2023, 11), converter.convertToEntityAttribute("2023-11"));
    }
}
