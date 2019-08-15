package org.wikimedia.commons.donvip.spacemedia.data.local.kari;

import java.time.LocalDate;
import java.util.Objects;

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
        return "KariMedia [id=" + id + ", " + (getAssetUrl() != null ? "url=" + getAssetUrl() + ", " : "")
                + (title != null ? "title=" + title + ", " : "") + (kariId != null ? "kariId=" + kariId + ", " : "")
                + (date != null ? "date=" + date + ", " : "")
                + (description != null ? "description=" + description + ", " : "")
                + (sha1 != null ? "sha1=" + sha1 : "") + "]";
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(id, kariId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        KariMedia other = (KariMedia) obj;
        return id == other.id && Objects.equals(kariId, other.kariId);
    }
}
