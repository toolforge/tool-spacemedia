package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

import java.util.List;

public class AbuseFilter {
    private int id;
    private String description;
    private List<String> actions;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }

    @Override
    public String toString() {
        return "AbuseFilter [id=" + id + ", " + (description != null ? "description=" + description + ", " : "")
                + (actions != null ? "actions=" + actions : "") + "]";
    }
}
