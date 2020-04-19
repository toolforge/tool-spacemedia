package org.wikimedia.commons.donvip.spacemedia.data.commons.api.rest;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserProfile {

    @JsonProperty("username")
    private String userName;

    @JsonProperty("editcount")
    private long editCount;

    @JsonProperty("confirmed_email")
    private boolean confirmedEmail;

    private boolean blocked;

    private long registered;

    private List<String> groups;

    private List<String> rights;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getEditCount() {
        return editCount;
    }

    public void setEditCount(long editCount) {
        this.editCount = editCount;
    }

    public boolean isConfirmedEmail() {
        return confirmedEmail;
    }

    public void setConfirmedEmail(boolean confirmedEmail) {
        this.confirmedEmail = confirmedEmail;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public long getRegistered() {
        return registered;
    }

    public void setRegistered(long registered) {
        this.registered = registered;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public List<String> getRights() {
        return rights;
    }

    public void setRights(List<String> rights) {
        this.rights = rights;
    }
}
