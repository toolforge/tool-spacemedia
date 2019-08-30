package org.wikimedia.commons.donvip.spacemedia.data.domain.kari;

import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;

@Entity
public class KariMedia extends Media<Integer> {

    @Id
    @NotNull
    private Integer id;

    private String kariId;

    private LocalDate date;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getKariId() {
        return kariId;
    }

    public void setKariId(String kariId) {
        this.kariId = kariId;
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
