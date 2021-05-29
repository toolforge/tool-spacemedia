package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa;

public class NasaResponse {

    private NasaCollection collection;

    public NasaCollection getCollection() {
        return collection;
    }

    public void setCollection(NasaCollection collection) {
        this.collection = collection;
    }

    @Override
    public String toString() {
        return "NasaResponse [" + (collection != null ? "collection=" + collection : "") + "]";
    }
}
