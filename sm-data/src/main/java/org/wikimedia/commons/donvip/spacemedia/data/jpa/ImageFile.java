package org.wikimedia.commons.donvip.spacemedia.data.jpa;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Entity
public class ImageFile extends File implements Visual {

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ImageFormat format;

    private short height;

    private short width;

    /**
     * Perceptual hash.
     */
    @Column(nullable = true, length = 52)
    private String phash;

    public ImageFormat getFormat() {
        return format;
    }

    public void setFormat(ImageFormat format) {
        this.format = format;
    }

    @Override
    public short getHeight() {
        return height;
    }

    @Override
    public void setHeight(short height) {
        this.height = height;
    }

    @Override
    public short getWidth() {
        return width;
    }

    @Override
    public void setWidth(short width) {
        this.width = width;
    }

    public String getPhash() {
        return phash;
    }

    public void setPhash(String phash) {
        this.phash = phash;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(format, phash);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        ImageFile other = (ImageFile) obj;
        return format == other.format && Objects.equals(phash, other.phash);
    }
}
