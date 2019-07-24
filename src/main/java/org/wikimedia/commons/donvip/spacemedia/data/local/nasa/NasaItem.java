package org.wikimedia.commons.donvip.spacemedia.data.local.nasa;

import java.net.URL;
import java.util.List;

public class NasaItem {

    private URL href;
    private List<NasaMedia> data;

    public URL getHref() {
        return href;
    }
    
    public void setHref(URL href) {
        this.href = href;
    }
    
    public List<NasaMedia> getData() {
        return data;
    }

    public void setData(List<NasaMedia> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "NasaItem [" + (href != null ? "href=" + href + ", " : "") + (data != null ? "data=" + data : "") + "]";
    }
}
