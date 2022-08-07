package org.wikimedia.commons.donvip.spacemedia.data.commons;

/**
 * Projection with only needed information, especially NOT the serialized
 * metadata which causes problems to the MariaDB JDBC driver.
 */
public interface CommonsImageProjection {

    String getName();

    String getSha1();

    String getTimestamp();
}
