package org.wikimedia.commons.donvip.spacemedia.service.twitter;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

record TweetRequest(TweetMedia media, String text) {

    static record TweetMedia(@JsonProperty("media_ids") List<String> mediaIds) {
    }
}