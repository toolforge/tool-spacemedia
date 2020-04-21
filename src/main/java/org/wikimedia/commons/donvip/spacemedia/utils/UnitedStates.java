package org.wikimedia.commons.donvip.spacemedia.utils;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UnitedStates {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnitedStates.class);

    /**
     * Pattern for US Military VIRIN identifiers. See
     * https://commons.wikimedia.org/wiki/Commons:VIRIN
     * https://www.dimoc.mil/Submit-DoD-VI/Digital-VI-Toolkit-read-first/Create-a-VIRIN/
     */
    private static final Pattern US_VIRIN = Pattern.compile(
            "([0-9]{6,8})-([A-Z])-([0-9A-Z]{5})-([0-9]{4})(?:-([A-Z]{2}))?");

    private UnitedStates() {

    }

    public static boolean isVirin(String identifier) {
        return US_VIRIN.matcher(identifier).matches();
    }

    public static List<String> getUsVirinTemplates(String virin, URL url) {
        return getUsVirinTemplates(virin, url.toExternalForm());
    }

    public static List<String> getUsVirinTemplates(String virin, String url) {
        Matcher m = US_VIRIN.matcher(virin);
        if (m.matches()) {
            String letter = m.group(2);
            switch (letter) {
            case "A":
                return List.of(virinTemplate(virin, "Army", url), "PD-USGov-Military-Army");
            case "D":
                return List.of(virinTemplate(virin, "Department of Defense", url), "PD-USGov-Military");
            case "F":
                return List.of(virinTemplate(virin, "Air Force", url), "PD-USGov-Military-Air Force");
            case "G":
                return List.of(virinTemplate(virin, "Coast Guard", url), "PD-USCG");
            case "H":
                return List.of(virinTemplate(virin, "Department of Homeland Security", url), "PD-USGov-DHS");
            case "M":
                return List.of(virinTemplate(virin, "Marine Corps", url), "PD-USGov-Military-Marines");
            case "N":
                return List.of(virinTemplate(virin, "Navy", url), "PD-USGov-Military-Navy");
            case "O":
                return List.of(virinTemplate(virin, "Other", url));
            case "P":
                return List.of(virinTemplate(virin, "Executive Office of the President", url), "PD-USGov-POTUS");
            case "S":
                return List.of(virinTemplate(virin, "Department of State", url), "PD-USGov-DOS");
            case "Z":
                return List.of(virinTemplate(virin, "National Guard", url), "PD-USGov-Military-National Guard");
            default:
                LOGGER.error("Unknown US military organization letter: {}", letter);
                return List.of(virinTemplate(virin, "Armed Forces", url));
            }
        }
        return Collections.emptyList();
    }

    private static String virinTemplate(String virin, String organization, String url) {
        return "ID-USMil |1=" + virin + " |2= " + organization + "|url= " + url;
    }
}
