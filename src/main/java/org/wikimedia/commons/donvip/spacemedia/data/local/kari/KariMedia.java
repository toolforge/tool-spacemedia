package org.wikimedia.commons.donvip.spacemedia.data.local.kari;

import java.net.URL;
import java.time.LocalDate;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.validation.constraints.NotNull;

import org.wikimedia.commons.donvip.spacemedia.data.local.Media;

@Entity
public class KariMedia extends Media {

    @Id
    @NotNull
    private int id;

    @NotNull
    private URL url;

    private String title;

    private String kariId;

    private LocalDate date;

    @Lob
    private String description;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getKariId() {
        return kariId;
    }

    public void setKariId(String kariId) {
        this.kariId = kariId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "KariMedia [id=" + id + ", " + (url != null ? "url=" + url + ", " : "")
                + (title != null ? "title=" + title + ", " : "") + (kariId != null ? "kariId=" + kariId + ", " : "")
                + (date != null ? "date=" + date + ", " : "")
                + (description != null ? "description=" + description + ", " : "")
                + (sha1 != null ? "sha1=" + sha1 : "") + "]";
    }
}
