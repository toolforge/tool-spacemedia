package org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity;

public enum DjangoplicityLicence {

    CC_BY_4_0("CC-BY 4.0");

    private final String text;

    private DjangoplicityLicence(String text) {
        this.text = text;
    }

    public static DjangoplicityLicence from(String text) {
        for (DjangoplicityLicence licence : values()) {
            if (licence.text.equals(text)) {
                return licence;
            }
        }
        throw new IllegalArgumentException(text);
    }
}
