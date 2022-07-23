package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

public class EditResponse {
    private String result;
    private int pageid;
    private String title;
    private String contentmodel;
    private int oldrevid;
    private int newrevid;
    private String newtimestamp;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public int getPageid() {
        return pageid;
    }

    public void setPageid(int pageid) {
        this.pageid = pageid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentmodel() {
        return contentmodel;
    }

    public void setContentmodel(String contentmodel) {
        this.contentmodel = contentmodel;
    }

    public int getOldrevid() {
        return oldrevid;
    }

    public void setOldrevid(int oldrevid) {
        this.oldrevid = oldrevid;
    }

    public int getNewrevid() {
        return newrevid;
    }

    public void setNewrevid(int newrevid) {
        this.newrevid = newrevid;
    }

    public String getNewtimestamp() {
        return newtimestamp;
    }

    public void setNewtimestamp(String newtimestamp) {
        this.newtimestamp = newtimestamp;
    }

    @Override
    public String toString() {
        return "EditResponse [" + (result != null ? "result=" + result + ", " : "") + "pageid=" + pageid + ", "
                + (title != null ? "title=" + title + ", " : "")
                + (contentmodel != null ? "contentmodel=" + contentmodel + ", " : "") + "oldrevid=" + oldrevid
                + ", newrevid=" + newrevid + ", " + (newtimestamp != null ? "newtimestamp=" + newtimestamp : "") + "]";
    }
}
