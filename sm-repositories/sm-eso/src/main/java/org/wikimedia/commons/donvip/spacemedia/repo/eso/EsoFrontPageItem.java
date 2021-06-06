package org.wikimedia.commons.donvip.spacemedia.repo.eso;

import java.net.URL;
import java.util.Objects;

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

    @Override
    public int hashCode() {
        return Objects.hash(height, id, potw, src, title, url, width);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EsoFrontPageItem other = (EsoFrontPageItem) obj;
        return height == other.height && Objects.equals(id, other.id) && Objects.equals(potw, other.potw)
                && Objects.equals(src, other.src) && Objects.equals(title, other.title)
                && Objects.equals(url, other.url) && width == other.width;
    }
}
