package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiError {

    private String code;

    private String info;

    private String errorclass;

    @JsonProperty("apierror-exceptioncaught")
    private String exceptioncaught;

    @JsonProperty("apierror-exceptioncaughttype")
    private String exceptioncaughttype;

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

    public String getErrorclass() {
        return errorclass;
    }

    public void setErrorclass(String errorclass) {
        this.errorclass = errorclass;
    }

    public String getExceptioncaught() {
        return exceptioncaught;
    }

    public void setExceptioncaught(String exceptioncaught) {
        this.exceptioncaught = exceptioncaught;
    }

    public String getExceptioncaughttype() {
        return exceptioncaughttype;
    }

    public void setExceptioncaughttype(String exceptioncaughttype) {
        this.exceptioncaughttype = exceptioncaughttype;
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
                + (errorclass != null ? "errorclass=" + errorclass + ", " : "")
                + (exceptioncaught != null ? "apierror-exceptioncaught=" + exceptioncaught + ", " : "")
                + (exceptioncaughttype != null ? "apierror-exceptioncaughttype=" + exceptioncaughttype + ", " : "")
                + (docref != null ? "docref=" + docref + ", " : "")
                + (abusefilter != null ? "abusefilter=" + abusefilter + ", " : "")
                + (star != null ? "star=" + star : "") + "]";
    }
}
