package org.wikimedia.commons.donvip.spacemedia.service.mastodon;

import java.util.Objects;

import com.github.scribejava.core.builder.api.DefaultApi20;

public class MastodonApi extends DefaultApi20 {
    private final String domain;

    protected MastodonApi(String domain) {
        this.domain = Objects.requireNonNull(domain, "domain");
    }

    public static MastodonApi instance(String domain) {
        return new MastodonApi(domain);
    }

    @Override
    public String getAccessTokenEndpoint() {
        return String.format("https://%s/oauth/token", domain);
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return String.format("https://%s/oauth/authorize", domain);
    }

    public String getMediaUrl() {
        return String.format("https://%s/api/v2/media", domain);
    }

    public String getStatusUrl() {
        return String.format("https://%s/api/v1/statuses", domain);
    }
}
