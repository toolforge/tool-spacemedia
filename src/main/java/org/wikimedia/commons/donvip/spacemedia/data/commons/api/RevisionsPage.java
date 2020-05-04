package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RevisionsPage {

    @JsonProperty("pageid")
    private int pageId;

    private short ns;

    private String title;

    private List<Revision> revisions;

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public short getNs() {
        return ns;
    }

    public void setNs(short ns) {
        this.ns = ns;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Revision> getRevisions() {
        return revisions;
    }

    public void setRevisions(List<Revision> revisions) {
        this.revisions = revisions;
    }
}
