package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response to the Hubble/James Webb {@code image} API call. See
 * <a href="http://hubblesite.org/api/documentation#images">documentation</a>.
 */
public class HubbleNasaImageResponse {

	/**
	 * Title given to the image.
	 */
	private String name;

	/**
	 * Image description text, caption.
	 */
	private String description;

	/**
	 * Image's credits and acknowledgments.
	 */
	private String credits;

	/**
	 * Legacy name given to this Image in a news release. Usually is 'a', 'b', 'c',
	 * ...
	 */
	@JsonProperty("news_name")
	private String newsName;

	/**
	 * Space Telescope or telescope website, the Image belongs to. It is usually
	 * 'hubble', 'james_webb', etc.
	 */
	private String mission;

	/**
	 * Collection name the Image belongs to.
	 */
	private String collection;

	/**
	 * List of downloadable image files. It could be images, PDFs, ZIP files
	 * containing images, etc.
	 */
	@JsonProperty("image_files")
	private List<HubbleNasaImageFiles> imageFiles;

	/**
	 * Returns the title given to the image.
	 * 
	 * @return the title given to the image
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns image description text, caption.
	 * 
	 * @return image description text, caption
	 */
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Returns image's credits and acknowledgments.
	 * 
	 * @return image's credits and acknowledgments
	 */
	public String getCredits() {
		return credits;
	}

	public void setCredits(String credits) {
		this.credits = credits;
	}

	/**
	 * Returns the legacy name given to this image in a news release. Usually is
	 * 'a', 'b', 'c', ...
	 * 
	 * @return the legacy name given to this image in a news release. Usually is
	 *         'a', 'b', 'c', ...
	 */
	public String getNewsName() {
		return newsName;
	}

	public void setNewsName(String newsName) {
		this.newsName = newsName;
	}

	/**
	 * Returns Space Telescope or telescope website, the image belongs to. It is
	 * usually 'hubble', 'james_webb', etc.
	 * 
	 * @return Space Telescope or telescope website, the image belongs to. It is
	 *         usually 'hubble', 'james_webb', etc.
	 */
	public String getMission() {
		return mission;
	}

	public void setMission(String mission) {
		this.mission = mission;
	}

	/**
	 * Returns the collection name the image belongs to.
	 * 
	 * @return the collection name the image belongs to
	 */
	public String getCollection() {
		return collection;
	}

	public void setCollection(String collection) {
		this.collection = collection;
	}

	/**
	 * Returns list of downloadable image files. It could be images, PDFs, ZIP files
	 * containing images, etc.
	 * 
	 * @return list of downloadable image files. It could be images, PDFs, ZIP files
	 *         containing images, etc
	 */
	public List<HubbleNasaImageFiles> getImageFiles() {
		return imageFiles;
	}

	public void setImageFiles(List<HubbleNasaImageFiles> imageFiles) {
		this.imageFiles = imageFiles;
	}

	@Override
	public String toString() {
		return "HubbleNasaImageResponse [name=" + name + ", mission=" + mission + ", collection=" + collection + "]";
	}
}
