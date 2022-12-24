package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

public class RevisionsQueryResponse extends ApiResponse {

    private RevisionsQuery query;

    public RevisionsQuery getQuery() {
        return query;
    }

    public void setQuery(RevisionsQuery query) {
        this.query = query;
    }
}
