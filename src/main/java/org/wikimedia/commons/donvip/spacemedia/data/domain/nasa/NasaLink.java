package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa;

import java.net.URL;

public class NasaLink {

    private String rel;
    private String prompt;
    private URL href;
    
    public String getRel() {
        return rel;
    }
    
    public void setRel(String rel) {
        this.rel = rel;
    }
    
    public String getPrompt() {
        return prompt;
    }
    
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
    
    public URL getHref() {
        return href;
    }
    
    public void setHref(URL href) {
        this.href = href;
    }

    @Override
    public String toString() {
        return "NasaLink [" + (rel != null ? "rel=" + rel + ", " : "")
                + (prompt != null ? "prompt=" + prompt + ", " : "") + (href != null ? "href=" + href : "") + "]";
    }
}
