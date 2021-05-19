package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import javax.persistence.Embeddable;

@Embeddable
public class DvidsImageDimensions {
    /**
     * Height of original image.
     */
    private Short height;

    /**
     * Width of original image.
     */
    private Short width;

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
}
