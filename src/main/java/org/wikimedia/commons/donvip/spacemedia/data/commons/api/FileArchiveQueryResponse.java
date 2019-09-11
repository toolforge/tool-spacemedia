package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

public class FileArchiveQueryResponse {

    private String batchcomplete;

    private FileArchiveQuery query;

    public String getBatchcomplete() {
        return batchcomplete;
    }

    public void setBatchcomplete(String batchcomplete) {
        this.batchcomplete = batchcomplete;
    }

    public FileArchiveQuery getQuery() {
        return query;
    }

    public void setQuery(FileArchiveQuery query) {
        this.query = query;
    }
}
