package org.wikimedia.commons.donvip.spacemedia.data.domain.youtube;

import java.time.Duration;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.SingleFileMedia;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity(name = "YoutubeVideo")
public class YouTubeVideo extends SingleFileMedia<String> {

    @Id
    @Column(nullable = false, length = 11)
    private String id;

    @Column(nullable = false, length = 24)
    @JsonProperty("channel_id")
    private String channelId;

    @Column(nullable = true)
    @JsonProperty("channel_title")
    private String channelTitle;

    @Column(nullable = false)
    private Duration duration;

    @Column(nullable = true)
    private Boolean caption;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

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
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(channelId, id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        YouTubeVideo other = (YouTubeVideo) obj;
        return Objects.equals(channelId, other.channelId) && Objects.equals(id, other.id);
    }

    @Override
    public String toString() {
        return "YouTubeVideo [id=" + id + ", channelId=" + channelId + "]";
    }
}
