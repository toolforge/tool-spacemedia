package org.wikimedia.commons.donvip.spacemedia.commons.api.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserInfo {

    private long id;

    private String name;

    private List<String> groups;

    private List<String> rights;

    @JsonProperty("ratelimits")
    private RateLimits rateLimits;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public RateLimits getRateLimits() {
        return rateLimits;
    }

    public void setRateLimits(RateLimits rateLimits) {
        this.rateLimits = rateLimits;
    }
}
