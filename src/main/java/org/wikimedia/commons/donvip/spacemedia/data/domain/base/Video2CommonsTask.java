package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import static java.util.Objects.requireNonNull;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

@Entity
public class Video2CommonsTask {

    @Id
    private String id;

    @Column(nullable = false, length = 2000)
    private URL url;

    @Column(nullable = false)
    private Long metadataId;

    @Column(nullable = false, length = 255)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Status status;

    @Column(nullable = false, columnDefinition = "TINYINT default 0")
    private int progress;

    @Lob
    @Column(nullable = true, columnDefinition = "MEDIUMTEXT")
    private String text;

    @Column(nullable = false)
    private ZonedDateTime created;

    @Column(nullable = true)
    private ZonedDateTime lastChecked;

    @Column(nullable = false, length = 32)
    private String orgId;

    @Embedded
    private CompositeMediaId mediaId;

    // https://github.com/toolforge/video2commons/blob/master/video2commons/frontend/api.py#L152
    public enum Status {
        PROGRESS, FAIL, ABORT, NEEDSSU, DONE;

        public boolean shouldSucceed() {
            return this == PROGRESS || this == DONE;
        }

        public boolean isCompleted() {
            return this == FAIL || this == DONE;
        }

        public boolean isFailed() {
            return this == FAIL;
        }

        public static Set<Status> incompleteStates() {
            return Set.of(PROGRESS);
        }
    }

    public Video2CommonsTask() {

    }

    public Video2CommonsTask(String id, URL url, String filename, String orgId, CompositeMediaId mediaId,
            Long metadataId) {
        setId(requireNonNull(id));
        setUrl(requireNonNull(url));
        setMetadataId(requireNonNull(metadataId));
        setFilename(requireNonNull(filename));
        setOrgId(requireNonNull(orgId));
        setMediaId(requireNonNull(mediaId));
        setCreated(ZonedDateTime.now());
        setStatus(Status.PROGRESS);
        setProgress(-1);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public Long getMetadataId() {
        return metadataId;
    }

    public void setMetadataId(Long metadataId) {
        this.metadataId = metadataId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setStatus(String status) {
        setStatus(Status.valueOf(status.toUpperCase(Locale.ENGLISH)));
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public ZonedDateTime getCreated() {
        return created;
    }

    public void setCreated(ZonedDateTime created) {
        this.created = created;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public CompositeMediaId getMediaId() {
        return mediaId;
    }

    public void setMediaId(CompositeMediaId mediaId) {
        this.mediaId = mediaId;
    }

    public ZonedDateTime getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(ZonedDateTime lastChecked) {
        this.lastChecked = lastChecked;
    }

    @JsonIgnore
    public boolean isNoAudioTrackError() {
        return getStatus().isFailed() && getText().contains("Audio is asked to be kept but the file has no audio");
    }

    @Override
    public String toString() {
        return "Video2CommonsTask [" + (id != null ? "id=" + id + ", " : "")
                + (filename != null ? "filename=" + filename + ", " : "")
                + (status != null ? "status=" + status + ", " : "")
                + "progress=" + progress + ", "
                + (created != null ? "created=" + created + ", " : "")
                + (lastChecked != null ? "lastChecked=" + lastChecked : "") + "]";
    }
}
