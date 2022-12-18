package org.wikimedia.commons.donvip.spacemedia.data.commons;

public enum CommonsMediaType {
    UNKNOWN,
    BITMAP,
    DRAWING,
    AUDIO,
    VIDEO,
    MULTIMEDIA,
    OFFICE,
    TEXT,
    EXECUTABLE,
    ARCHIVE,
    THREE_D("3D");

    private final String value;

    private CommonsMediaType() {
        this(null);
    }

    private CommonsMediaType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value != null ? value : name();
    }

    public static CommonsMediaType of(String value) {
        for (CommonsMediaType v : values()) {
            if (v.getValue().equals(value)) {
                return v;
            }
        }
        return null;
    }
}
