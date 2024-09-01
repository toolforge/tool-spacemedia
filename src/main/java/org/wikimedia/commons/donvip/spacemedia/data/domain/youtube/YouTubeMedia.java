package org.wikimedia.commons.donvip.spacemedia.data.domain.youtube;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.SingleFileMedia;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity(name = "YoutubeMedia")
public class YouTubeMedia extends SingleFileMedia {

    @Column(nullable = true)
    @JsonProperty("channel_title")
    private String channelTitle;

    @Column(nullable = true)
    private Boolean caption;

    public String getChannelTitle() {
        return channelTitle;
    }

    public void setChannelTitle(String channelTitle) {
        this.channelTitle = channelTitle;
    }

    public Boolean isCaption() {
        return caption;
    }

    public void setCaption(Boolean caption) {
        this.caption = caption;
    }

    @Override
    public boolean isAudio() {
        return false;
    }

    @Override
    public boolean isImage() {
        return false;
    }

    @Override
    public boolean isVideo() {
        return true;
    }

    @Override
    public String toString() {
        return "YouTubeMedia [id=" + getId() + ']';
    }
}
