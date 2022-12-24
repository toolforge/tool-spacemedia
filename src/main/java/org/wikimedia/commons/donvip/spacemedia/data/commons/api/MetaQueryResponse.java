package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

public class MetaQueryResponse extends ApiResponse {

    private String batchcomplete;

    private MetaQuery query;

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

    @Override
    public String toString() {
        return "MetaQueryResponse [" + (batchcomplete != null ? "batchcomplete=" + batchcomplete + ", " : "")
                + (query != null ? "query=" + query + ", " : "")
                + (getError() != null ? "error=" + getError() + ", " : "")
                + (getServedBy() != null ? "servedBy=" + getServedBy() : "") + "]";
    }
}
