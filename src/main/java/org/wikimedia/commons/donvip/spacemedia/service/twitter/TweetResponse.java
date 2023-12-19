package org.wikimedia.commons.donvip.spacemedia.service.twitter;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

record TweetResponse(TweetResponse.TweetData data) {

    static record TweetData(@JsonProperty("edit_history_tweet_ids") List<Long> editHistoryTweetIds, Long id,
            String text) {
    }
}