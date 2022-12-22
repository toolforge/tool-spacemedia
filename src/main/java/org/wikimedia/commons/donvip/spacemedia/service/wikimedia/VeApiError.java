package org.wikimedia.commons.donvip.spacemedia.service.wikimedia;

public class VeApiError {
    private String code;
    private String info;
    private String docref;

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

    public String getDocref() {
        return docref;
    }

    public void setDocref(String docref) {
        this.docref = docref;
    }

    @Override
    public String toString() {
        return "VeApiError [" + (code != null ? "code=" + code + ", " : "")
                + (info != null ? "info=" + info + ", " : "") + (docref != null ? "docref=" + docref : "") + "]";
    }
}