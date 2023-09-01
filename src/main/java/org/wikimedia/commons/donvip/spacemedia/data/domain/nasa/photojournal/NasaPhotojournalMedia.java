package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.photojournal;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithKeywords;

@Entity
@Indexed
public class NasaPhotojournalMedia extends Media implements WithKeywords {

    private static final Pattern FIGURE = Pattern.compile("PIA\\d+_fig[^\\.]+\\..+", Pattern.CASE_INSENSITIVE);

    @Column(name = "nasa_id", nullable = true, length = 48)
    private String nasaId;

    @Column(name = "target", nullable = true, length = 50)
    private String target;

    @Column(name = "mission", nullable = true, length = 90)
    private String mission;

    @Column(name = "spacecraft", nullable = true, length = 50)
    private String spacecraft;

    @Column(name = "instrument", nullable = true, length = 100)
    private String instrument;

    @Column(name = "producer", nullable = true, length = 64)
    private String producer;

    @Column(name = "big")
    private boolean big;

    @Lob
    @Column(name = "credit", nullable = false, columnDefinition = "TEXT")
    private String credit;

    @Lob
    @Column(name = "legend", nullable = true, columnDefinition = "TEXT")
    private String legend;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> keywords = new HashSet<>();

    public String getNasaId() {
        return nasaId;
    }

    public void setNasaId(String nasaId) {
        this.nasaId = nasaId;
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

    @Override
    public Set<String> getKeywords() {
        return keywords;
    }

    @Override
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

    public String getLegend() {
        return legend;
    }

    public void setLegend(String legend) {
        this.legend = legend;
    }

    @Override
    public String getUploadId(FileMetadata fileMetadata) {
        String file = fileMetadata.getAssetUrl().getFile();
        if (file.contains("/")) {
            file = file.substring(file.lastIndexOf('/') + 1);
        }
        return FIGURE.matcher(file).matches()
                ? file.substring(0, file.indexOf('.')).replace("_fullres", "").replace("_FIG", "_fig")
                : super.getUploadId(fileMetadata);
    }

    @Override
    public String toString() {
        return "NasaPhotojournalMedia [id=" + getId() + ", nasaId=" + nasaId + ", "
                + (target != null ? "target=" + target + ", " : "")
                + (mission != null ? "mission=" + mission + ", " : "")
                + (spacecraft != null ? "spacecraft=" + spacecraft + ", " : "")
                + (instrument != null ? "instrument=" + instrument + ", " : "")
                + (producer != null ? "producer=" + producer : "") + ']';
    }

    public NasaPhotojournalMedia copyDataFrom(NasaPhotojournalMedia mediaFromApi) {
        super.copyDataFrom(mediaFromApi);
        setNasaId(mediaFromApi.getNasaId());
        setTarget(mediaFromApi.getTarget());
        setMission(mediaFromApi.getMission());
        setSpacecraft(mediaFromApi.getSpacecraft());
        setInstrument(mediaFromApi.getInstrument());
        setProducer(mediaFromApi.getProducer());
        setKeywords(mediaFromApi.getKeywords());
        setBig(mediaFromApi.isBig());
        setCredit(mediaFromApi.getCredit());
        setLegend(mediaFromApi.getLegend());
        return this;
    }
}
