package org.wikimedia.commons.donvip.spacemedia.service.twitter;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

class TweetResponse {
    private TweetResponse.TweetData data;

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

    public TweetResponse.TweetData getData() {
        return data;
    }

    public void setData(TweetResponse.TweetData data) {
        this.data = data;
    }
}