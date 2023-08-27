package org.wikimedia.commons.donvip.spacemedia.data.domain.kari;

import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.SingleFileMedia;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Indexed
public class KariMedia extends SingleFileMedia<String> {

    private static final Pattern ID_REGEX = Pattern.compile("P_[^_]*_([^_]*)_(\\d{2})(\\d{2})(\\d{2})_(\\d{2})(\\d{2})",
            Pattern.CASE_INSENSITIVE);

    @Id
    @Column(nullable = false)
    private String id;

    @JsonProperty("kari_id")
    private String kariId;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getKariId() {
        return kariId;
    }

    public void setKariId(String kariId) {
        this.kariId = kariId;
    }

    @Override
    @Transient
    public LocalDate getCreationDate() {
        Matcher m = ID_REGEX.matcher(kariId);
        if (m.matches()) {
            int year = 2000 + Integer.parseInt(m.group(2));
            if (year > LocalDate.now().getYear()) {
                year -= 100;
            }
            return LocalDate.of(year, Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)));
        }
        return null;
    }

    @Transient
    public String getMission() {
        Matcher m = ID_REGEX.matcher(kariId);
        return m.matches() ? m.group(1) : "";
    }

    @Override
    public String toString() {
        return "KariMedia [id=" + id + ", "
                + (title != null ? "title=" + title + ", " : "") + (kariId != null ? "kariId=" + kariId + ", " : "")
                + (description != null ? "description=" + description + ", " : "")
                + "metadata=" + getMetadata() + "]";
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

    public KariMedia copyDataFrom(KariMedia mediaFromApi) {
        super.copyDataFrom(mediaFromApi);
        this.kariId = mediaFromApi.kariId;
        return this;
    }
}
