package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

public class EditApiResponse extends ApiResponse {

    private EditResponse edit;

    public EditResponse getEdit() {
        return edit;
    }

    public void setEdit(EditResponse edit) {
        this.edit = edit;
    }

    @Override
    public String toString() {
        return "EditApiResponse [" + (edit != null ? "edit=" + edit + ", " : "")
                + (getError() != null ? "error=" + getError() + ", " : "")
                + (getServedBy() != null ? "servedby=" + getServedBy() : "")
                + "]";
    }
}
