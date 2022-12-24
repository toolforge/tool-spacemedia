package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiError {

    private String code;

    private String info;

    private String docref;

    private AbuseFilter abusefilter;

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

    public AbuseFilter getAbusefilter() {
        return abusefilter;
    }

    public void setAbusefilter(AbuseFilter abusefilter) {
        this.abusefilter = abusefilter;
    }

    public String getStar() {
        return star;
    }

    public void setStar(String star) {
        this.star = star;
    }

    public String getDocref() {
        return docref;
    }

    public void setDocref(String docref) {
        this.docref = docref;
    }

    @Override
    public String toString() {
        return "ApiError [" + (code != null ? "code=" + code + ", " : "") + (info != null ? "info=" + info + ", " : "")
                + (abusefilter != null ? "abusefilter=" + abusefilter + ", " : "")
                + (docref != null ? "docref=" + docref + ", " : "")
                + (star != null ? "star=" + star : "") + "]";
    }
}
