package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import static java.util.Objects.requireNonNull;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

@Entity
public class Video2CommonsTask {

    @Id
    private String id;

    @Column(nullable = false, length = 540)
    private URL url;

    @Column(nullable = false, length = 255)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Status status;

    @Column(nullable = false, columnDefinition = "TINYINT default 0")
    private int progress;

    @Column(nullable = false)
    private ZonedDateTime created;

    @Column(nullable = true)
    private ZonedDateTime lastChecked;

    public enum Status {
        PENDING, PROGRESS, SUCCESS, FAILURE, RETRY, ABORTED, DONE;

        public boolean shouldSucceed() {
            return this == PENDING || this == PROGRESS || this == SUCCESS || this == RETRY || this == DONE;
        }

        public boolean isCompleted() {
            return this == SUCCESS || this == FAILURE || this == DONE;
        }

        public static Set<Status> incompleteStates() {
            return Set.of(PENDING, PROGRESS, RETRY);
        }
    }

    public Video2CommonsTask() {

    }

    public Video2CommonsTask(String id, URL url, String filename) {
        setId(requireNonNull(id));
        setUrl(requireNonNull(url));
        setFilename(requireNonNull(filename));
        setCreated(ZonedDateTime.now());
        setStatus(Status.PENDING);
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
        this.status = Status.valueOf(status.toUpperCase(Locale.ENGLISH));
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

    public ZonedDateTime getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(ZonedDateTime lastChecked) {
        this.lastChecked = lastChecked;
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
