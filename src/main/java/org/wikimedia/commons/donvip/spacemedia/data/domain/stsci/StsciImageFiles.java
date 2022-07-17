package org.wikimedia.commons.donvip.spacemedia.data.domain.stsci;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Array of downloadable image files. It could be images, PDFs, ZIP files
 * containing images, etc. See
 * <a href="http://hubblesite.org/api/documentation#images">documentation</a>.
 */
public class StsciImageFiles {

    /**
     * HTTPS URL of the image file.
     */
    @JsonProperty("file_url")
    private String fileUrl;

    /**
     * Size of the file.
     */
    @JsonProperty("file_size")
    private int fileSize;

    /**
     * Width of the image, if it is a common image file format
     */
    private int width;

    /**
     * Height of the image, if it is an image file
     */
    private int height;

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
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

    @Override
    public String toString() {
        return fileUrl;
    }
}
