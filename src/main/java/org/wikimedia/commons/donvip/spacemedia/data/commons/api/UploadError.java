package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UploadError {

    private String code;

    private String info;

    @JsonProperty("*")
    private String star;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getStar() {
        return star;
    }

    public void setStar(String star) {
        this.star = star;
    }

    @Override
    public String toString() {
        return "UploadError [" + (code != null ? "code=" + code + ", " : "")
                + (info != null ? "info=" + info + ", " : "")
                + (star != null ? "*=" + star : "")
                + "]";
    }
}
