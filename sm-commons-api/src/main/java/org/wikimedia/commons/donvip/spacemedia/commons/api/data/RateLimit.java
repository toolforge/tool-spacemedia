package org.wikimedia.commons.donvip.spacemedia.commons.api.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RateLimit {

    private Limit user;

    @JsonProperty("autopatrolled")
    private Limit autoPatrolled;

    public Limit getUser() {
        return user;
    }

    public void setUser(Limit user) {
        this.user = user;
    }

    public Limit getAutoPatrolled() {
        return autoPatrolled;
    }

    public void setAutoPatrolled(Limit autoPatrolled) {
        this.autoPatrolled = autoPatrolled;
    }
}
