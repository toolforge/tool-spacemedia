package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiPageInfo {

    @JsonProperty("total_results")
    private int totalResults;

    @JsonProperty("results_per_page")
    private int resultsPerPage;

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    public int getResultsPerPage() {
        return resultsPerPage;
    }

    public void setResultsPerPage(int resultsPerPage) {
        this.resultsPerPage = resultsPerPage;
    }

    @Override
    public String toString() {
        return "ApiPageInfo [totalResults=" + totalResults + ", resultsPerPage=" + resultsPerPage + "]";
    }
}
