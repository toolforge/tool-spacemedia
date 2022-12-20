package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.photojournal;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResExtraMedia;

@Entity
@Indexed
@Table(indexes = { @Index(columnList = "sha1, full_res_sha1, extra_sha1, phash, full_res_phash, extra_phash") })
public class NasaPhotojournalMedia extends FullResExtraMedia<String, ZonedDateTime> {

    @Id
    @Column(name = "pia_id", nullable = false, length = 10)
    private String piaId;

    @Column(name = "nasa_id", nullable = true, length = 50)
    private String nasaId;

    @Column(name = "photo_date", nullable = false)
    private ZonedDateTime date;

    @Column(name = "target", nullable = true, length = 50)
    private String target;

    @Column(name = "mission", nullable = true, length = 100)
    private String mission;

    @Column(name = "spacecraft", nullable = true, length = 100)
    private String spacecraft;

    @Column(name = "instrument", nullable = true, length = 100)
    private String instrument;

    @Column(name = "producer", nullable = true, length = 100)
    private String producer;

    @Column(name = "big")
    private boolean big;

    @Lob
    @Column(name = "credit", nullable = false, columnDefinition = "TEXT")
    private String credit;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> keywords = new HashSet<>();

    @Override
    public String getId() {
        return piaId;
    }

    @Override
    public void setId(String id) {
        this.piaId = id;
    }

    public String getNasaId() {
        return nasaId;
    }

    public void setNasaId(String nasaId) {
        this.nasaId = nasaId;
    }

    @Override
    public ZonedDateTime getDate() {
        return date;
    }

    @Override
    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getMission() {
        return mission;
    }

    public void setMission(String mission) {
        this.mission = mission;
    }

    public String getSpacecraft() {
        return spacecraft;
    }

    public void setSpacecraft(String spacecraft) {
        this.spacecraft = spacecraft;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    public Set<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }

    public boolean isBig() {
        return big;
    }

    public void setBig(boolean big) {
        this.big = big;
    }

    public String getCredit() {
        return credit;
    }

    public void setCredit(String credit) {
        this.credit = credit;
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

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(piaId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        NasaPhotojournalMedia other = (NasaPhotojournalMedia) obj;
        return Objects.equals(piaId, other.piaId);
    }

    @Override
    public String toString() {
        return "NasaPhotojournalMedia [id=" + piaId + ", nasaId=" + nasaId + ", "
                + "date=" + date + ", " + (target != null ? "target=" + target + ", " : "")
                + (mission != null ? "mission=" + mission + ", " : "")
                + (spacecraft != null ? "spacecraft=" + spacecraft + ", " : "")
                + (instrument != null ? "instrument=" + instrument + ", " : "")
                + (producer != null ? "producer=" + producer : "") + ']';
    }
}
