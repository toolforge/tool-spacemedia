package org.wikimedia.commons.donvip.spacemedia.service.wikimedia;

public class VisualEditorResponse {
    private String result;
    private String content;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "VisualEditorResponse [" + (result != null ? "result=" + result + ", " : "")
                + (content != null ? "content=" + content : "") + "]";
    }
}