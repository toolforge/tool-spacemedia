package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ApiPageInfo(
        @JsonProperty("total_results") int totalResults,
        @JsonProperty("results_per_page") int resultsPerPage) {

}
