package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library;

import java.net.URL;
import java.util.List;

public class NasaCollection {

    private NasaMetadata metadata;
    private URL href;
    private List<NasaLink> links;
    private String version;
    private List<NasaItem> items;

    public NasaMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(NasaMetadata metadata) {
        this.metadata = metadata;
    }

    public URL getHref() {
        return href;
    }

    public void setHref(URL href) {
        this.href = href;
    }

    public List<NasaLink> getLinks() {
        return links;
    }

    public void setLinks(List<NasaLink> links) {
        this.links = links;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<NasaItem> getItems() {
        return items;
    }

    public void setItems(List<NasaItem> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        return "NasaCollection [" + (metadata != null ? "metadata=" + metadata + ", " : "")
                + (href != null ? "href=" + href + ", " : "") + (version != null ? "version=" + version + ", " : "")
                + (items != null ? "items=" + items.subList(0, Math.min(items.size(), maxLen)) : "") + "]";
    }
}
