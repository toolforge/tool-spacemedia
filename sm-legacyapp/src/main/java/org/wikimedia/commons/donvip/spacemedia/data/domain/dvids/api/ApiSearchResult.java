package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api;

public class ApiSearchResult {

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "ApiSearchResult [id=" + id + "]";
    }
}
