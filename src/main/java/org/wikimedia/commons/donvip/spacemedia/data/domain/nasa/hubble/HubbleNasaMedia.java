package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble;

import java.time.LocalDate;
import java.time.ZonedDateTime;
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

import org.hibernate.search.annotations.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMedia;

@Entity
@Indexed
@Table(indexes = { @Index(columnList = "sha1, full_res_sha1, phash, full_res_phash") })
public class HubbleNasaMedia extends FullResMedia<Integer, ZonedDateTime> {

    @Id
    private Integer id;

	@Column(length = 9)
	private String newsId;

	@Column(name = "release_date", nullable = false)
	private ZonedDateTime date;

	@Column(name = "exposure_date", nullable = true)
	private LocalDate exposureDate;

	@Column(length = 255)
	private String objectName;

	/**
	 * Image's credits and acknowledgments.
	 */
	@Lob
	@Column(columnDefinition = "TEXT")
	private String credits;

	/**
	 * Space Telescope or telescope website, the Image belongs to. It is usually
	 * 'hubble', 'james_webb', etc.
	 */
	@Column(length = 10)
	private String mission;

	@ElementCollection(fetch = FetchType.EAGER)
	private Set<String> keywords;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

	public String getNewsId() {
		return newsId;
	}

	public void setNewsId(String newsId) {
		this.newsId = newsId;
	}

	@Override
	public ZonedDateTime getDate() {
        return date;
    }

    @Override
	public void setDate(ZonedDateTime date) {
        this.date = date;
    }

	public LocalDate getExposureDate() {
		return exposureDate;
	}

	public void setExposureDate(LocalDate exposureDate) {
		this.exposureDate = exposureDate;
	}

	public String getCredits() {
		return credits;
	}

	public void setCredits(String credits) {
		this.credits = credits;
	}

	public String getMission() {
		return mission;
	}

	public void setMission(String mission) {
		this.mission = mission;
	}

	public Set<String> getKeywords() {
		return keywords;
	}

	public void setKeywords(Set<String> keywords) {
		this.keywords = keywords;
	}

	public String getObjectName() {
		return objectName;
	}

	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

	@Override
	public int hashCode() {
		return 31 * super.hashCode() + Objects.hash(credits, date, id, mission, newsId, keywords, objectName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj) || getClass() != obj.getClass())
			return false;
		HubbleNasaMedia other = (HubbleNasaMedia) obj;
		return Objects.equals(credits, other.credits) && Objects.equals(date, other.date)
				&& Objects.equals(id, other.id) && Objects.equals(mission, other.mission)
				&& Objects.equals(newsId, other.newsId) && Objects.equals(keywords, other.keywords)
				&& Objects.equals(objectName, other.objectName);
	}

	@Override
	public String toString() {
		return "HubbleNasaMedia [id=" + id + ", newsId=" + newsId + ", date=" + date + ", objectName=" + objectName
				+ ", mission=" + mission + "]";
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
