package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

public class MetaQueryResponse {

    private String batchcomplete;

    private MetaQuery query;

    private ApiError error;

    private String servedby;

    public String getBatchcomplete() {
        return batchcomplete;
    }

    public void setBatchcomplete(String batchcomplete) {
        this.batchcomplete = batchcomplete;
    }

    public MetaQuery getQuery() {
        return query;
    }

    public void setQuery(MetaQuery query) {
        this.query = query;
    }

    public ApiError getError() {
        return error;
    }

    public void setError(ApiError error) {
        this.error = error;
    }

    public String getServedby() {
        return servedby;
    }

    public void setServedby(String servedby) {
        this.servedby = servedby;
    }
}
