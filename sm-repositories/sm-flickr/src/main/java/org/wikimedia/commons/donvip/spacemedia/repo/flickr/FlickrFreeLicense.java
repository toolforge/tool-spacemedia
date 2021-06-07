package org.wikimedia.commons.donvip.spacemedia.repo.flickr;

import static org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Licence.CC0_1_0;
import static org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Licence.CC_BY_2_0;
import static org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Licence.CC_BY_SA_2_0;
import static org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Licence.FLICKR_NO_KNOWN_COPYRIGHT_RESTRICTIONS;
import static org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Licence.FLICKR_PUBLIC_DOMAIN_MARK;
import static org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Licence.PD_US_GOV;

import java.util.Objects;

import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Licence;

/**
 * https://commons.wikimedia.org/wiki/Commons:Flickr_files
 */
public enum FlickrFreeLicense {
    Attribution_License(4, CC_BY_2_0, "Cc-by-2.0"),                                                                // https://creativecommons.org/licenses/by/2.0/
    Attribution_ShareAlike_License(5, CC_BY_SA_2_0, "Cc-by-sa-2.0"),                                            // https://creativecommons.org/licenses/by-sa/2.0/
    No_known_copyright_restrictions(7, FLICKR_NO_KNOWN_COPYRIGHT_RESTRICTIONS, "Flickr-no known copyright restrictions"), // https://www.flickr.com/commons/usage/
    United_States_Government_Work(8, PD_US_GOV, "PD-USGov"),                                                                // https://www.usa.gov/copyright.shtml
    Public_Domain_Dedication_CC0(9, CC0_1_0, "Cc-zero"),                                                     // https://creativecommons.org/publicdomain/zero/1.0/
    Public_Domain_Mark(10, FLICKR_PUBLIC_DOMAIN_MARK, "Flickr-public domain mark");                          // https://creativecommons.org/publicdomain/mark/1.0/

    private final int code;
    private final Licence licence;
    private final String template;

    private FlickrFreeLicense(int code, Licence licence, String template) {
        this.code = code;
        this.licence = licence;
        this.template = Objects.requireNonNull(template);
    }

    public int getCode() {
        return code;
    }

    public Licence getLicence() {
        return licence;
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
