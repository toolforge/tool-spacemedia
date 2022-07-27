package org.wikimedia.commons.donvip.spacemedia.utils;

import java.net.URL;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UnitedStates {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnitedStates.class);

    /**
     * Exact pattern for US Military VIRIN identifiers. See
     * https://commons.wikimedia.org/wiki/Commons:VIRIN
     * https://www.dimoc.mil/Submit-DoD-VI/Digital-VI-Toolkit-read-first/Create-a-VIRIN/
     */
    private static final Pattern US_VIRIN = Pattern.compile(
            "([0-9]{6})-([A-Z])-([0-9A-Z]{5})-([0-9]{3,4})(?:-([A-Z]{2}))?");

    /**
     * Fake pattern for US Military VIRIN identifiers as found on Flickr.
     */
    private static final Pattern FAKE_US_VIRIN = Pattern.compile(
            "([0-9]{8})-([A-Z])-([0-9A-Z]{5})-([0-9]{3,4})");

    private UnitedStates() {

    }

    public static boolean isClearPublicDomain(String description) {
        return description != null
                && (description.contains("U.S. Army photo")
                    || description.contains("U.S. Navy photo")
                    || description.contains("U.S. Air Force photo")
                    || description.contains("U.S. Marine Corps photo")
                    || description.contains("U.S. Coast Guard photo")
                    || description.contains("U.S. Space Force photo"));
    }

    public static boolean isFakeVirin(String identifier) {
        return FAKE_US_VIRIN.matcher(identifier).matches();
    }

    public static boolean isVirin(String identifier) {
        return US_VIRIN.matcher(identifier).matches();
    }

    public static class VirinTemplates {
        private final String virinTemplate;
        private final String pdTemplate;

        VirinTemplates(String virinTemplate, String pdTemplate) {
            this.virinTemplate = Objects.requireNonNull(virinTemplate);
            this.pdTemplate = pdTemplate;
        }

        public String getVirinTemplate() {
            return virinTemplate;
        }

        public String getPdTemplate() {
            return pdTemplate;
        }
    }

    public static VirinTemplates getUsVirinTemplates(String virin, URL url) {
        return getUsVirinTemplates(virin, url.toExternalForm());
    }

    public static VirinTemplates getUsVirinTemplates(String virin, String url) {
        Matcher m = US_VIRIN.matcher(virin);
        if (m.matches()) {
            String letter = m.group(2);
            switch (letter) {
            case "A":
                return new VirinTemplates(virinTemplate(virin, "Army", url), "PD-USGov-Military-Army");
            case "D":
                return new VirinTemplates(virinTemplate(virin, "Department of Defense", url), "PD-USGov-Military");
            case "F":
                return new VirinTemplates(virinTemplate(virin, "Air Force", url), "PD-USGov-Military-Air Force");
            case "G":
                return new VirinTemplates(virinTemplate(virin, "Coast Guard", url), "PD-USCG");
            case "H":
                return new VirinTemplates(virinTemplate(virin, "Department of Homeland Security", url), "PD-USGov-DHS");
            case "M":
                return new VirinTemplates(virinTemplate(virin, "Marine Corps", url), "PD-USGov-Military-Marines");
            case "N":
                return new VirinTemplates(virinTemplate(virin, "Navy", url), "PD-USGov-Military-Navy");
            case "O":
                return new VirinTemplates(virinTemplate(virin, "Other", url), null);
            case "P":
                return new VirinTemplates(virinTemplate(virin, "Executive Office of the President", url), "PD-USGov-POTUS");
            case "S":
                return new VirinTemplates(virinTemplate(virin, "Department of State", url), "PD-USGov-DOS");
            case "X":
                return new VirinTemplates(virinTemplate(virin, "Space Force", url), "PD-USGov-Military-Space Force");
            case "Z":
                return new VirinTemplates(virinTemplate(virin, "National Guard", url), "PD-USGov-Military-National Guard");
            default:
                LOGGER.error("Unknown US military organization letter: {}", letter);
                return new VirinTemplates(virinTemplate(virin, "Armed Forces", url), null);
            }
        }
        return null;
    }

    private static String virinTemplate(String virin, String organization, String url) {
        return "ID-USMil |1=" + virin + " |2= " + organization + "|url= " + url;
    }
}
