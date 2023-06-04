package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.util.Objects;

/**
 * Projection of {@link CommonsImage} with only needed information, especially
 * NOT the serialized metadata which causes problems to the MariaDB JDBC driver.
 *
 * Must be a DTO class: the interface-style projection requests all fields from
 * the database.
 */
public class CommonsImageProjection {

    private final String name;
    private final String sha1;
    private final String timestamp;
    private final int width;
    private final int height;
    private final CommonsMediaType mediaType;
    private final CommonsMajorMime majorMime;
    private final String minorMime;

    public CommonsImageProjection(String name, String sha1, String timestamp, int width, int height,
            CommonsMediaType mediaType, CommonsMajorMime majorMime, String minorMime) {
        this.name = name;
        this.sha1 = sha1;
        this.timestamp = timestamp;
        this.width = width;
        this.height = height;
        this.mediaType = mediaType;
        this.majorMime = majorMime;
        this.minorMime = minorMime;
    }

    public String getName() {
        return name;
    }

    public String getSha1() {
        return sha1;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public CommonsMediaType getMediaType() {
        return mediaType;
    }

    public CommonsMajorMime getMajorMime() {
        return majorMime;
    }

    public String getMinorMime() {
        return minorMime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, sha1, timestamp, width, height, mediaType, majorMime, minorMime);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        CommonsImageProjection other = (CommonsImageProjection) obj;
        return Objects.equals(name, other.name) && Objects.equals(sha1, other.sha1)
                && Objects.equals(timestamp, other.timestamp) && width == other.width && height == other.height
                && mediaType == other.mediaType && majorMime == other.majorMime
                && Objects.equals(minorMime, other.minorMime);
    }

    @Override
    public String toString() {
        return "CommonsImageProjection [" + (name != null ? "name=" + name + ", " : "")
                + (sha1 != null ? "sha1=" + sha1 + ", " : "")
                + (timestamp != null ? "timestamp=" + timestamp + ", " : "") + "width=" + width + ", height=" + height
                + ", mediaType=" + mediaType + ", majorMime=" + majorMime + ", minorMime=" + minorMime
                + ']';
    }
}
