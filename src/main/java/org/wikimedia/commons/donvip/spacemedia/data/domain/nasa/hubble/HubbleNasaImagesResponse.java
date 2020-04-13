package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response to the Hubble/James Webb {@code images} API call. See
 * <a href="http://hubblesite.org/api/documentation#images">documentation</a>.
 */
public class HubbleNasaImagesResponse {

	/**
	 * Internal key to identify the image. It can be used to gather more information
	 * using the details API call (below).
	 */
	private int id;

	/**
	 * Name given to the Image
	 */
	private String name;

	/**
	 * Legacy name given to this image in a news release. Usually is 'a', 'b', 'c',
	 * ...
	 */
	@JsonProperty("news_name")
	private String newsName;

	/**
	 * Collection name the image belongs to.
	 */
	private String collection;

	/**
	 * Space Telescope or telescope website, the image belongs to. It is usually
	 * 'hubble', 'james_webb', etc.
	 */
	private String mission;

	/**
	 * Returns the internal key to identify the image. It can be used to gather more
	 * information using the details API call (below).
	 * 
	 * @return the internal key to identify the image
	 */
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Returns the name given to the Image.
	 * 
	 * @return the name given to the Image
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the legacy name given to this image in a news release. Usually is
	 * 'a', 'b', 'c', ...
	 * 
	 * @return the legacy name given to this image in a news release
	 */
	public String getNewsName() {
		return newsName;
	}

	public void setNewsName(String newsName) {
		this.newsName = newsName;
	}

	/**
	 * Returns the collection name the image belongs to.
	 * 
	 * @return the collection name the image belongs to.
	 */
	public String getCollection() {
		return collection;
	}

	public void setCollection(String collection) {
		this.collection = collection;
	}

	/**
	 * Returns the Space Telescope or telescope website, the image belongs to. It is
	 * usually 'hubble', 'james_webb', etc.
	 * 
	 * @return the Space Telescope or telescope website, the image belongs to. It is
	 *         usually 'hubble', 'james_webb', etc.
	 */
	public String getMission() {
		return mission;
	}

	public void setMission(String mission) {
		this.mission = mission;
	}

	@Override
	public String toString() {
		return "HubbleNasaImagesResponse [id=" + id + ", name=" + name + ", collection=" + collection + ", mission="
				+ mission + "]";
	}
}
