package org.wikimedia.commons.donvip.spacemedia.data.domain.eso;

import java.net.URL;

public class EsoFrontPageItem {
    private String id;
    private String title;
    private int width;
    private int height;
    private URL src;
    private String url;
    private String potw;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public URL getSrc() {
        return src;
    }

    public void setSrc(URL src) {
        this.src = src;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPotw() {
        return potw;
    }

    public void setPotw(String potw) {
        this.potw = potw;
    }
}