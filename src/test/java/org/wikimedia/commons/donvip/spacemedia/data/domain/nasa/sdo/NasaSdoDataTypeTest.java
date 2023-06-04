package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NasaSdoDataTypeTest {

    @ParameterizedTest
    @CsvSource({ "_0094,AIA,94", "_4500,AIA,4500", "_HMIB,HMI,6173", "_HMIIF,HMI,6173" })
    void testValues(NasaSdoDataType value, NasaSdoInstrument instrument, int wavelength) {
        assertEquals(instrument, value.getInstrument());
        assertEquals(wavelength, value.getWavelength());
    }
}
