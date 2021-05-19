package org.wikimedia.commons.donvip.spacemedia.commons.api.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MetaQuery {
    private Tokens tokens;

    @JsonProperty("userinfo")
    private UserInfo userInfo;

    public Tokens getTokens() {
        return tokens;
    }

    public void setTokens(Tokens tokens) {
        this.tokens = tokens;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }
}
