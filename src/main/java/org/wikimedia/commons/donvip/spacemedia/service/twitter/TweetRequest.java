package org.wikimedia.commons.donvip.spacemedia.service.twitter;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

record TweetRequest(String text, TweetMedia media) {

    static record TweetMedia(@JsonProperty("media_ids") List<String> mediaIds) {
    }
}