package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

public class QueryResponse<T> extends ApiResponse {

    private String batchcomplete;

    private T query;

    public String getBatchcomplete() {
        return batchcomplete;
    }

    public void setBatchcomplete(String batchcomplete) {
        this.batchcomplete = batchcomplete;
    }

    public T getQuery() {
        return query;
    }

    public void setQuery(T query) {
        this.query = query;
    }
}
