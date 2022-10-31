package org.wikimedia.commons.donvip.spacemedia.data.domain.eso;

public enum EsoLicence {

    CC_BY_4_0("CC-BY 4.0");

    private final String text;

    private EsoLicence(String text) {
        this.text = text;
    }

    public static EsoLicence from(String text) {
        for (EsoLicence licence : values()) {
            if (licence.text.equals(text)) {
                return licence;
            }
        }
        throw new IllegalArgumentException(text);
    }
}
