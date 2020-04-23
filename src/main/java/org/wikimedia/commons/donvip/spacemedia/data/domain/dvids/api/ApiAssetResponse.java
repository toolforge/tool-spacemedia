package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api;

import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMedia;

public class ApiAssetResponse {

    private DvidsMedia results;

    public DvidsMedia getResults() {
        return results;
    }

    public void setResults(DvidsMedia results) {
        this.results = results;
    }

    @Override
    public String toString() {
        return "ApiAssetResponse [" + results + "]";
    }
}
