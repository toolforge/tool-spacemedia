package org.wikimedia.commons.donvip.spacemedia.service;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.apis.TwitterApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;

@Service
public class TwitterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TwitterService.class);

    @Autowired
    private ObjectMapper jackson;

    private OAuth10aService oAuthService;
    private OAuth1AccessToken oAuthAccessToken;

    public TwitterService(
            @Value("${twitter.api.oauth1.consumer-token}") String consumerToken,
            @Value("${twitter.api.oauth1.consumer-secret}") String consumerSecret,
            @Value("${twitter.api.oauth1.access-token}") String accessToken,
            @Value("${twitter.api.oauth1.access-secret}") String accessSecret) {
        try {
            oAuthService = new ServiceBuilder(consumerToken).apiSecret(consumerSecret).build(TwitterApi.instance());
            oAuthAccessToken = new OAuth1AccessToken(accessToken, accessSecret);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Unable to setup Twitter API: {}", e.getMessage(), e);
        }
    }

    public TweetResponse tweet(Collection<? extends Media<?, ?>> uploadedMedia, Set<String> twitterAccounts)
            throws IOException {
        if (oAuthService == null || oAuthAccessToken == null) {
            throw new IOException("Twitter API not initialized correctly");
        }
        try {
            Response response = oAuthService.execute(buildTweetRequest(uploadedMedia, twitterAccounts));
            if (response.getCode() >= 400) {
                LOGGER.error("Twitter error: {}", response);
            } else {
                LOGGER.info("Twitter response: {}", response);
            }
            return jackson.readValue(response.getBody(), TweetResponse.class);
        } catch (ExecutionException e) {
            throw new IOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    OAuthRequest buildTweetRequest(Collection<? extends Media<?, ?>> uploadedMedia, Set<String> twitterAccounts)
            throws JsonProcessingException {
        OAuthRequest request = new OAuthRequest(Verb.POST, "https://api.twitter.com/2/tweets");
        request.addHeader("Content-Type", "application/json");
        request.setCharset(StandardCharsets.UTF_8.name());
        oAuthService.signRequest(oAuthAccessToken, request);
        int size = uploadedMedia.size();
        String text = String.format("%d new picture%s", size, size >= 2 ? "s" : "");
        if (!twitterAccounts.isEmpty()) {
            text += " from " + twitterAccounts.stream().sorted().map(account -> "@" + account).collect(joining(" "));
        }
        request.setPayload(jackson.writeValueAsString(new TweetRequest(text)));
        return request;
    }

    static class TweetRequest {
        private String text;

        public TweetRequest() {
            // Default constructor for jackson
        }

        public TweetRequest(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    static class TweetResponse {
        private TweetData data;

        static class TweetData {
            @JsonProperty("edit_history_tweet_ids")
            private List<Long> editHistoryTweetIds;
            private Long id;
            private String text;

            public List<Long> getEditHistoryTweetIds() {
                return editHistoryTweetIds;
            }

            public void setEditHistoryTweetIds(List<Long> editHistoryTweetIds) {
                this.editHistoryTweetIds = editHistoryTweetIds;
            }

            public Long getId() {
                return id;
            }

            public void setId(Long id) {
                this.id = id;
            }

            public String getText() {
                return text;
            }

            public void setText(String text) {
                this.text = text;
            }
        }

        public TweetData getData() {
            return data;
        }

        public void setData(TweetData data) {
            this.data = data;
        }
    }
}
