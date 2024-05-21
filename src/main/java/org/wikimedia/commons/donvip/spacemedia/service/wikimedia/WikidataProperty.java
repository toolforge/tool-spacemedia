package org.wikimedia.commons.donvip.spacemedia.service.wikimedia;

public enum WikidataProperty {

    /**
     * that class of which this subject is a particular example and member;
     * different from P279 (subclass of); for example: K2 is an instance of
     * mountain; volcano is a subclass of mountain (and an instance of volcanic
     * landform)
     */
    P31_INSTANCE_OF(31),

    /**
     * the area of the celestial sphere of which the subject is a part (from a
     * scientific standpoint, not an astrological one)
     */
    P59_CONSTELLATION(59),

    /**
     * maker of this creative work or other object (where no more specific property
     * exists)
     */
    P170_CREATOR(170),

    /**
     * entity visually depicted in an image, literarily described in a work, or
     * otherwise incorporated into an audiovisual or other medium; see also P921,
     * 'main subject'
     */
    P180_DEPICTS(180),

    /**
     * name of the Wikimedia Commons category containing files related to this item
     * (without the prefix "Category:")
     */
    P373_COMMONS_CATEGORY(373),

    /** catalog name of an object, use with qualifier P972 */
    P528_CATALOG_CODE(528),

    /** part of full name of person */
    P734_FAMILY_NAME(734),

    /**
     * place where the item was conceived or made; where applicable, location of
     * final assembly
     */
    P1071_LOCATION_OF_CREATION(1071),

    /** official name of the subject in its official language(s) */
    P1448_OFFICIAL_NAME(1448),

    /**
     * label for the items in their official language (P37) or their original
     * language (P364)
     */
    P1705_NATIVE_LABEL(1705),

    /**
     * short name of a place, organisation, person, journal, instrument, etc.
     */
    P1813_SHORT_NAME(1813),

    /**
     * method, process or technique used to grow, cook, weave, build, assemble,
     * manufacture the item
     */
    P2079_FABRICATION_METHOD(2079),

    /**
     * equipment (e.g. model of camera, lens microphone), used to capture this
     * image, video, audio, or data
     */
    P4082_CAPTURED_WITH(4082);

    private final int code;

    private WikidataProperty(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "P" + code;
    }
}
