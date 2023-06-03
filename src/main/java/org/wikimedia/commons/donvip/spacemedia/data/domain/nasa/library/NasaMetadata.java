package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NasaMetadata {

    @JsonProperty("total_hits")
    private int totalHits;

    public int getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(int totalHits) {
        this.totalHits = totalHits;
    }

    @Override
    public String toString() {
        return "NasaMetadata [totalHits=" + totalHits + "]";
    }
}
