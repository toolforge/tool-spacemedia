package org.wikimedia.commons.donvip.spacemedia.data.domain.kari;

import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.search.annotations.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;

@Entity
@Indexed
@Table(indexes = { @Index(columnList = "sha1, phash") })
public class KariMedia extends Media<Integer, LocalDate> {

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

    @Override
    public LocalDate getDate() {
        return date;
    }

    @Override
    public void setDate(LocalDate date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "KariMedia [id=" + id + ", " 
                + (title != null ? "title=" + title + ", " : "") + (kariId != null ? "kariId=" + kariId + ", " : "")
                + (date != null ? "date=" + date + ", " : "")
                + (description != null ? "description=" + description + ", " : "")
                + "metadata=" + metadata + "]";
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

    @Override
    public boolean isAudio() {
        return false;
    }

    @Override
    public boolean isImage() {
        return true;
    }

    @Override
    public boolean isVideo() {
        return false;
    }
}
