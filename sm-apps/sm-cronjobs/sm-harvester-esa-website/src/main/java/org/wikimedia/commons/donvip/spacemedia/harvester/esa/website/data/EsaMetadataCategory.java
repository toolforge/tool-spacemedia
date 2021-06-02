package org.wikimedia.commons.donvip.spacemedia.harvester.esa.website.data;

import java.util.Set;

public enum EsaMetadataCategory {
    ACTION("action"),
    ACTIVITY("activity", "landmark"),
    MISSION("mission", "rocket"),
    PEOPLE("people"),
    PHOTOSET("set", "tags"),
    SYSTEMS(true, "system", "book"),
    LOCATIONS(true, "location"),
    KEYWORDS(true, "keywords");

    private final Set<String> markers;

    private final boolean multiValues;

    private EsaMetadataCategory(String... markers) {
        this(false, markers);
    }

    private EsaMetadataCategory(boolean multiValues, String... markers) {
        this.multiValues = multiValues;
        this.markers = Set.of(markers);
    }

    public Set<String> getMarkers() {
        return markers;
    }

    public boolean isMultiValues() {
        return multiValues;
    }
}
