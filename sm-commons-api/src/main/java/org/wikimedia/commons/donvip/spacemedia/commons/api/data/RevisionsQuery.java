package org.wikimedia.commons.donvip.spacemedia.commons.api.data;

import java.util.Map;

public class RevisionsQuery {
    private Map<Integer, RevisionsPage> pages;

    public Map<Integer, RevisionsPage> getPages() {
        return pages;
    }

    public void setPages(Map<Integer, RevisionsPage> pages) {
        this.pages = pages;
    }
}
