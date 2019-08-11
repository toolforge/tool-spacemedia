package org.wikimedia.commons.donvip.spacemedia.data.local;

import java.util.Objects;

public class Statistics implements Comparable<Statistics> {
    private final String agency;
    private final long totalMedia;
    private final long missingMedia;

    public Statistics(String agency, long totalMedia, long missingMedia) {
        this.agency = Objects.requireNonNull(agency);
        this.totalMedia = totalMedia;
        this.missingMedia = missingMedia;
    }

    /**
     * @return the agency
     */
    public String getAgency() {
        return agency;
    }

    /**
     * @return the totalMedia
     */
    public long getTotalMedia() {
        return totalMedia;
    }

    /**
     * @return the missingMedia
     */
    public long getMissingMedia() {
        return missingMedia;
    }

    @Override
    public int compareTo(Statistics o) {
        return agency.compareTo(o.agency);
    }
}
