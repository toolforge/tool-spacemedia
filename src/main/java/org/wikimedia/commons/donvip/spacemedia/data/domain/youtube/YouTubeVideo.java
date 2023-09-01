package org.wikimedia.commons.donvip.spacemedia.data.domain.youtube;

import java.time.Duration;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.SingleFileMedia;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity(name = "YoutubeVideo")
public class YouTubeVideo extends SingleFileMedia {

    @Column(nullable = true)
    @JsonProperty("channel_title")
    private String channelTitle;

    @Column(nullable = false)
    private Duration duration;

    @Column(nullable = true)
    private Boolean caption;

    public String getChannelTitle() {
        return channelTitle;
    }

    public void setChannelTitle(String channelTitle) {
        this.channelTitle = channelTitle;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
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
        return "YouTubeVideo [id=" + getId() + ']';
    }
}
