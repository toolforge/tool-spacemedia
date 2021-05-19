package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiSearchResponse {

    private ApiErrors errors;

    @JsonProperty("page_info")
    private ApiPageInfo pageInfo;

    private List<ApiSearchResult> results;

    public ApiErrors getErrors() {
        return errors;
    }

    public void setErrors(ApiErrors errors) {
        this.errors = errors;
    }

    public ApiPageInfo getPageInfo() {
        return pageInfo;
    }

    public void setPageInfo(ApiPageInfo pageInfo) {
        this.pageInfo = pageInfo;
    }

    public List<ApiSearchResult> getResults() {
        return results;
    }

    public void setResults(List<ApiSearchResult> results) {
        this.results = results;
    }

    @Override
    public String toString() {
        return "ApiResponse [" + (errors != null ? "errors=" + errors + ", " : "")
                + (pageInfo != null ? "pageInfo=" + pageInfo + ", " : "")
                + (results != null ? "results=" + results : "") + "]";
    }
}
