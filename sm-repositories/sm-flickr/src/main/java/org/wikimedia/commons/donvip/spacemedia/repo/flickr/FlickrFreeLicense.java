package org.wikimedia.commons.donvip.spacemedia.repo.flickr;

import java.util.Objects;

/**
 * https://commons.wikimedia.org/wiki/Commons:Flickr_files
 */
public enum FlickrFreeLicense {
    Attribution_License(4, "Cc-by-2.0"),                                          // https://creativecommons.org/licenses/by/2.0/
    Attribution_ShareAlike_License(5, "Cc-by-sa-2.0"),                            // https://creativecommons.org/licenses/by-sa/2.0/
    No_known_copyright_restrictions(7, "Flickr-no known copyright restrictions"), // https://www.flickr.com/commons/usage/
    United_States_Government_Work(8, "PD-USGov"),                                 // https://www.usa.gov/copyright.shtml
    Public_Domain_Dedication_CC0(9, "Cc-zero"),                                   // https://creativecommons.org/publicdomain/zero/1.0/
    Public_Domain_Mark(10, "Flickr-public domain mark");                          // https://creativecommons.org/publicdomain/mark/1.0/

    private final int code;
    private final String template;

    private FlickrFreeLicense(int code, String template) {
        this.code = code;
        this.template = Objects.requireNonNull(template);
    }

    public int getCode() {
        return code;
    }

    public String getWikiTemplate() {
        return template;
    }

    public static FlickrFreeLicense of(int code) {
        for (FlickrFreeLicense v : values()) {
            if (code == v.code) {
                return v;
            }
        }
        throw new IllegalArgumentException(Integer.toString(code));
    }
}
