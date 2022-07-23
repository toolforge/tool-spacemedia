package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

public class EditApiResponse {

    private EditResponse edit;

    private ApiError error;

    private String servedby;

    public EditResponse getEdit() {
        return edit;
    }

    public void setEdit(EditResponse edit) {
        this.edit = edit;
    }

    public ApiError getError() {
        return error;
    }

    public void setError(ApiError error) {
        this.error = error;
    }

    public String getServedby() {
        return servedby;
    }

    public void setServedby(String servedby) {
        this.servedby = servedby;
    }

    @Override
    public String toString() {
        return "EditApiResponse [" + (edit != null ? "edit=" + edit + ", " : "")
                + (error != null ? "error=" + error + ", " : "") + (servedby != null ? "servedby=" + servedby : "")
                + "]";
    }
}
