package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.net.URL;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;

import org.hibernate.search.annotations.Indexed;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Entity
@Indexed
public class DvidsVideo extends DvidsMedia {

    /**
     * Aspect ratio of the asset.
     */
    @JsonProperty("aspect_ratio")
    private DvidsAspectRatio aspectRatio;

    /**
     * Length of asset in seconds.
     */
    private Short duration;

    /**
     * List of mp4 files associated with asset
     */
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<DvidsVideoFile> files;

    /**
     * Url to master m3u8 playlist for asset if video has been encoded for HLS playback
     */
    @JsonProperty("hls_url")
    private URL hlsUrl;

    /**
     * Time at which the content actual content of the video is estimated to start
     */
    @JsonProperty("time_start")
    private Float timeStart;

    /**
     * Url to full resolution frame capture from asset
     */
    private URL image;

    public URL getImage() {
        return image;
    }

    public void setImage(URL image) {
        this.image = image;
    }

    public DvidsAspectRatio getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(DvidsAspectRatio aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public Short getDuration() {
        return duration;
    }

    public void setDuration(Short duration) {
        this.duration = duration;
    }

    @Override
    @JsonDeserialize(using = DvidsThumbnailDeserializer.class)
    public void setThumbnailUrl(URL thumbnailUrl) {
        super.setThumbnailUrl(thumbnailUrl);
    }

    public Set<DvidsVideoFile> getFiles() {
        return files;
    }

    public void setFiles(Set<DvidsVideoFile> files) {
        this.files = files;
        this.metadata.setAssetUrl(files.stream().max(Comparator.comparing(DvidsVideoFile::getSize))
                .map(DvidsVideoFile::getSrc).orElse(null));
    }

    public URL getHlsUrl() {
        return hlsUrl;
    }

    public void setHlsUrl(URL hlsUrl) {
        this.hlsUrl = hlsUrl;
    }

    public Float getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(Float timeStart) {
        this.timeStart = timeStart;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(aspectRatio, duration, files, hlsUrl, timeStart);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        DvidsVideo other = (DvidsVideo) obj;
        return aspectRatio == other.aspectRatio
                && Objects.equals(duration, other.duration) && Objects.equals(files, other.files)
                && Objects.equals(hlsUrl, other.hlsUrl) && Objects.equals(timeStart, other.timeStart);
    }

    @Override
    public String toString() {
        return "DvidsVideo ["
                + (getId() != null ? "id=" + getId() + ", " : "")
                + (getAspectRatio() != null ? "aspectRatio=" + getAspectRatio() + ", " : "")
                + (getTitle() != null ? "title=" + getTitle() + ", " : "")
                + (getDatePublished() != null ? "datePublished=" + getDatePublished() + ", " : "")
                + (getDate() != null ? "date=" + getDate() + ", " : "")
                + (getDescription() != null ? "description=" + getDescription() : "") + "]";
    }
}
