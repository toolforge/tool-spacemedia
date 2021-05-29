package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api;

public class ApiError {

    private String noRecordFound;

    private String notInArray;

    public String getNoRecordFound() {
        return noRecordFound;
    }

    public void setNoRecordFound(String noRecordFound) {
        this.noRecordFound = noRecordFound;
    }

    public String getNotInArray() {
        return notInArray;
    }

    public void setNotInArray(String notInArray) {
        this.notInArray = notInArray;
    }

    @Override
    public String toString() {
        return "ApiError [" + (noRecordFound != null ? "noRecordFound=" + noRecordFound + ", " : "")
                + (notInArray != null ? "notInArray=" + notInArray : "") + "]";
    }
}
