package org.wikimedia.commons.donvip.spacemedia.data.domain.eso;

import java.net.URL;

public class EsoFrontPageItem {
    /** ESO identifier. Example: 'eso1907a' */ 
    private String id;
    /** Image title. Example: 'First Image of a Black Hole' */
    private String title;
    /** Image width in pixels. Example: 7416 */
    private int width;
    /** Image height in pixels. Example: 4320 */
    private int height;
    /** Image thumbnail URL. Example: 'https://cdn.eso.org/images/thumb300y/eso1907a.jpg' */
    private URL src;
    /** Image relative link. Example: '/public/images/eso1907a/' */
    private String url;
    /** ? No idea. Empty string in example above */
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