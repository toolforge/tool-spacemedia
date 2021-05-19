package org.wikimedia.commons.donvip.spacemedia.commons.api.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UploadResponse {

    private String result;

    private String filename;

    @JsonProperty("imageinfo")
    private ImageInfo imageInfo;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public ImageInfo getImageInfo() {
        return imageInfo;
    }

    public void setImageInfo(ImageInfo imageInfo) {
        this.imageInfo = imageInfo;
    }

    @Override
    public String toString() {
        return "UploadResponse [result=" + result + ", filename=" + filename + ", imageInfo=" + imageInfo + "]";
    }
}
