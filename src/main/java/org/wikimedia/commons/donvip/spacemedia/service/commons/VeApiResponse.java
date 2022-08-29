package org.wikimedia.commons.donvip.spacemedia.service.commons;

public class VeApiResponse {
    private VisualEditorResponse visualeditor;
    private VeApiError error;

    public VisualEditorResponse getVisualeditor() {
        return visualeditor;
    }

    public void setVisualeditor(VisualEditorResponse visualeditor) {
        this.visualeditor = visualeditor;
    }

    public VeApiError getError() {
        return error;
    }

    public void setError(VeApiError error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "VeApiResponse [" + (visualeditor != null ? "visualeditor=" + visualeditor + ", " : "")
                + (error != null ? "error=" + error : "") + "]";
    }
}