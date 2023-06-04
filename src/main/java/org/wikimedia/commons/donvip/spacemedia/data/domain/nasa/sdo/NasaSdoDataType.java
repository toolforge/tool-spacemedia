package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo;

import java.util.Arrays;
import java.util.List;

public enum NasaSdoDataType {

    _0094, _0131, _0171, _0193, _0211, _0304, _0335, _1600, _1700, _4500,
    /** HMI magnetogram */
    _HMIB,
    /** HMI colorized magnetogram */
    _HMIBC,
    /** HMI dopplergram */
    _HMID,
    /** HMI intensitygram */
    _HMII,
    /** HMI intensitygram - colored */
    _HMIIC,
    /** HMI intensitygram - flattened */
    _HMIIF;

    public NasaSdoInstrument getInstrument() {
        return name().contains("HMI") ? NasaSdoInstrument.HMI : NasaSdoInstrument.AIA;
    }

    public int getWavelength() {
        return name().contains("HMI") ? 6173 : Integer.parseInt(name().substring(1));
    }

    public static List<NasaSdoDataType> values(NasaSdoInstrument instrument) {
        return Arrays.stream(values()).filter(v -> v.getInstrument() == instrument).toList();
    }
}
