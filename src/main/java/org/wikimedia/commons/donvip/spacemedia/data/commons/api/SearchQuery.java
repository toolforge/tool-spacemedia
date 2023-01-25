package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchQuery {

    @JsonProperty("searchinfo")
    private SearchInfo searchInfo;

    private Map<Long, WikiPage> pages;

    public SearchInfo getSearchInfo() {
        return searchInfo;
    }

    public void setSearchInfo(SearchInfo searchInfo) {
        this.searchInfo = searchInfo;
    }

    public Map<Long, WikiPage> getPages() {
        return pages;
    }

    public void setPages(Map<Long, WikiPage> pages) {
        this.pages = pages;
    }
}
