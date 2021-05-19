package org.wikimedia.commons.donvip.spacemedia.commons.api.data;

public class MetaQueryResponse {

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
}
