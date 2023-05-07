package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo;

public enum NasaSdoInstrument {

    AIA("Q118189296"), HMI("Q118189301"), EVE("Q118189304");

    private final String qid;

    private NasaSdoInstrument(String qid) {
        this.qid = qid;
    }

    public String getQid() {
        return qid;
    }
}
