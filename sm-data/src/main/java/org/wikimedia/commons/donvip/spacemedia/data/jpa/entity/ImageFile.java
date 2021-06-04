package org.wikimedia.commons.donvip.spacemedia.data.jpa.entity;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(value = "I")
public class ImageFile extends File implements Visual {

    private int height;

    private int width;

    /**
     * Perceptual hash.
     */
    @Column(length = 52)
    private String phash;

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
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
        return 31 * super.hashCode() + Objects.hash(phash);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        ImageFile other = (ImageFile) obj;
        return Objects.equals(phash, other.phash);
    }
}
