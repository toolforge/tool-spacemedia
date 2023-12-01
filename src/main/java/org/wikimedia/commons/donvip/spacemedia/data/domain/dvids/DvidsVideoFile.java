package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.net.URL;
import java.util.Objects;

import jakarta.persistence.Embeddable;

@Embeddable
public class DvidsVideoFile {

    private URL src;

    private String type;

    private Short height;

    private Short width;

    private Long size;

    private Short bitrate;

    public URL getSrc() {
        return src;
    }

    public void setSrc(URL src) {
        this.src = src;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Short getHeight() {
        return height;
    }

    public void setHeight(Short height) {
        this.height = height;
    }

    public Short getWidth() {
        return width;
    }

    public void setWidth(Short width) {
        this.width = width;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Short getBitrate() {
        return bitrate;
    }

    public void setBitrate(Short bitrate) {
        this.bitrate = bitrate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bitrate, height, size, src, type, width);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        DvidsVideoFile other = (DvidsVideoFile) obj;
        return Objects.equals(bitrate, other.bitrate) && Objects.equals(height, other.height)
                && Objects.equals(size, other.size) && Objects.equals(src, other.src)
                && Objects.equals(type, other.type) && Objects.equals(width, other.width);
    }
}
