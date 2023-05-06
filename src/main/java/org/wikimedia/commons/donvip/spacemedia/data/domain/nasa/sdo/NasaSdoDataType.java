package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo;

public enum NasaSdoDataType {

    _0094, _0131, _0171, _0193, _0211, _0304, _0335, _1600, _1700, _4500, _HMIB, _HMIBC, _HMID, _HMII, _HMIIC, _HMIIF;

    public NasaSdoInstrument getInstrument() {
        return name().contains("HMI") ? NasaSdoInstrument.HMI : NasaSdoInstrument.AIA;
    }
}
