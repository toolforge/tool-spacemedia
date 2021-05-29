package org.wikimedia.commons.donvip.spacemedia.data.domain.youtube;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;

@Entity(name = "YoutubeVideo")
public class YouTubeVideo extends Media<String, Instant> {

    @Id
    @Column(nullable = false, length = 11)
    private String id;

    @Column(nullable = false, name = "published_date")
    private Instant date;

    @Column(nullable = false, length = 24)
    private String channelId;

    @Column(nullable = true)
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

    @Override
    public Instant getDate() {
        return date;
    }

    @Override
    public void setDate(Instant publishedDate) {
        this.date = publishedDate;
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
        return 31 * super.hashCode() + Objects.hash(channelId, id, date);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        YouTubeVideo other = (YouTubeVideo) obj;
        return Objects.equals(channelId, other.channelId) && Objects.equals(id, other.id)
                && Objects.equals(date, other.date);
    }

    @Override
    public String toString() {
        return "YouTubeVideo [id=" + id + ", publishedDate=" + date + ", channelId=" + channelId + "]";
    }
}
