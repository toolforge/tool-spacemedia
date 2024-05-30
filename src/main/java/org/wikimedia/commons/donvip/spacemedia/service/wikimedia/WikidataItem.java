package org.wikimedia.commons.donvip.spacemedia.service.wikimedia;

public enum WikidataItem {

    /**
     * visual representation of a concept space; symbolic depiction emphasizing
     * relationships between elements of some space, such as objects, regions, or
     * themes
     */
    Q4006_MAP(4006),

    /** instrument that aids in the observation of remote objects */
    Q4213_TELESCOPE(4213),

    /** large landmass identified by convention */
    Q5107_CONTINENT(5107),

    /** distinct territorial body or political entity */
    Q6256_COUNTRY(6256),

    /**
     * one of the 88 divisions of the celestial sphere, defined by the IAU, many of
     * which derive from traditional asterisms
     */
    Q8928_CONSTELLATION(8928),

    /** image created by light falling on a light-sensitive surface */
    Q125191_PHOTOGRAPH(125191),

    /**
     * imagery of the Earth or another astronomical object taken from an artificial
     * satellite
     */
    Q725252_SATELLITE_IMAGERY(725252),

    /**
     * equipment specifically designed to facilitate the acquisition of scientific
     * data
     */
    Q3099911_SCIENTIFIC_INSTUMENT(3099911),

    /**
     * two-dimensional representation of the approaching and receding motions of an
     * object or area
     */
    Q5297355_DOPPLERGRAM(5297355),

    /** specific video with defining traits like framerate */
    Q98069877_VIDEO(98069877),

    /** graphic of the solar magnetic field */
    Q115801008_MAGNETOGRAM(115801008),

    /** graphic of the solar light intensity */
    Q119021644_INTENSITYGRAM(119021644),

    /**
     * each instance is a subclass in the hierarchy under <astronomical
     * object>(Q6999); such a subclass's instances in turn are particular identified
     * objects in Our Universe
     */
    Q17444909_ASTRONOMICAL_OBJECT_TYPE(17444909);

    private final int code;

    private WikidataItem(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "Q" + code;
    }
}
