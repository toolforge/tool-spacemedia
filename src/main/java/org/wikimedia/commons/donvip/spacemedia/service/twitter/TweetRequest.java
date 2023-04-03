package org.wikimedia.commons.donvip.spacemedia.service.twitter;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

class TweetRequest {
    private TweetRequest.TweetMedia media;
    private String text;

    public TweetRequest() {
        // Default constructor for jackson
    }

    public TweetRequest(TweetRequest.TweetMedia media, String text) {
        this.media = media;
        this.text = text;
    }

    public TweetRequest.TweetMedia getMedia() {
        return media;
    }

    public void setMedia(TweetRequest.TweetMedia media) {
        this.media = media;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    static class TweetMedia {
        @JsonProperty("media_ids")
        private List<Long> mediaIds;

        public TweetMedia() {
            // Default constructor for jackson
        }

        public TweetMedia(List<Long> mediaIds) {
            this.mediaIds = mediaIds;
        }

        public List<Long> getMediaIds() {
            return mediaIds;
        }

        public void setMediaIds(List<Long> mediaIds) {
            this.mediaIds = mediaIds;
        }
    }
}