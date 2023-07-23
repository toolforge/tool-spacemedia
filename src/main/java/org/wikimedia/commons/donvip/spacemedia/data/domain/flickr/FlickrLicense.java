package org.wikimedia.commons.donvip.spacemedia.data.domain.flickr;

import java.util.Objects;

/**
 * https://commons.wikimedia.org/wiki/Commons:Flickr_files
 * https://www.flickr.com/services/api/explore/flickr.photos.licenses.getInfo
 */
public enum FlickrLicense {
    AllRightsReserved(0, false, "flickr-unfree-but"),
    Attribution_NonCommercial_ShareAlike_License(1, false, "flickr-unfree-but"),
    Attribution_NonCommercial_License(2, false, "flickr-unfree-but"),
    Attribution_NonCommercial_NoDerivs_License(3, false, "flickr-unfree-but"),
    Attribution_License(4, true, "Cc-by-2.0"),                                          // https://creativecommons.org/licenses/by/2.0/
    Attribution_ShareAlike_License(5, true, "Cc-by-sa-2.0"),                            // https://creativecommons.org/licenses/by-sa/2.0/
    Attribution_NoDerivs_License(6, false, "flickr-unfree-but"),
    No_known_copyright_restrictions(7, true, "Flickr-no known copyright restrictions"), // https://www.flickr.com/commons/usage/
    United_States_Government_Work(8, true, "PD-USGov"),                                 // https://www.usa.gov/copyright.shtml
    Public_Domain_Dedication_CC0(9, true, "Cc-zero"),                                   // https://creativecommons.org/publicdomain/zero/1.0/
    Public_Domain_Mark(10, true, "Flickr-public domain mark");                          // https://creativecommons.org/publicdomain/mark/1.0/

    private final int code;
    private final boolean free;
    private final String template;

    private FlickrLicense(int code, boolean free, String template) {
        this.code = code;
        this.free = free;
        this.template = Objects.requireNonNull(template);
    }

    public int getCode() {
        return code;
    }

    public boolean isFree() {
        return free;
    }

    public String getWikiTemplate() {
        return template;
    }

    public static FlickrLicense of(int code) {
        for (FlickrLicense v : values()) {
            if (code == v.code) {
                return v;
            }
        }
        throw new IllegalArgumentException(Integer.toString(code));
    }
}
