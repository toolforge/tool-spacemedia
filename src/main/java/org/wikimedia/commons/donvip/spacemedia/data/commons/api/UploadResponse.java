package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UploadResponse {

    private long offset;

    private String result;

    private String filename;

    private String filekey;

    @JsonProperty("imageinfo")
    private ImageInfo imageInfo;

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

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

    public String getFilekey() {
        return filekey;
    }

    public void setFilekey(String filekey) {
        this.filekey = filekey;
    }

    @Override
    public String toString() {
        return "UploadResponse [offset=" + offset + ", " + (result != null ? "result=" + result + ", " : "")
                + (filename != null ? "filename=" + filename + ", " : "")
                + (filekey != null ? "filekey=" + filekey + ", " : "")
                + (imageInfo != null ? "imageInfo=" + imageInfo : "") + ']';
    }
}
