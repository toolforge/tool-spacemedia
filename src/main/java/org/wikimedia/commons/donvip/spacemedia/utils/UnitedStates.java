package org.wikimedia.commons.donvip.spacemedia.utils;

import static java.util.Optional.ofNullable;

import java.net.URL;
import java.time.Year;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;

public final class UnitedStates {

    /**
     * Exact pattern for US Military VIRIN identifiers. See
     * https://commons.wikimedia.org/wiki/Commons:VIRIN
     * https://www.dimoc.mil/Submit-DoD-VI/Digital-VI-Toolkit-read-first/Create-a-VIRIN/
     */
    private static final Pattern US_VIRIN = Pattern.compile(
            "([\\d]{6})-([A-Z])-([0-9A-Z]{5})-([\\d]{3,4})(?:-([A-Z]{2}))?");

    /**
     * Fake pattern for US Military VIRIN identifiers as found on Flickr.
     */
    private static final Pattern FAKE_US_VIRIN = Pattern.compile(
            "([\\d]{8})-([A-Z])-([0-9A-Z]{5})-([\\d]{3,4})");

    private UnitedStates() {

    }

    public static boolean isClearPublicDomain(String description) {
        return description != null
                && (description.contains("U.S. Army photo")
                    || description.contains("U.S. Navy photo")
                    || description.contains("U.S. Air Force photo")
                    || description.contains("U.S. Marine Corps photo")
                    || description.contains("U.S. Coast Guard photo")
                    || description.contains("U.S. Space Force photo")
                    || description.contains("dds.cr.usgs.gov")
                    || description.contains("eol.jsc.nasa.gov"));
    }

    public static boolean isFakeVirin(String identifier) {
        return FAKE_US_VIRIN.matcher(identifier).matches();
    }

    public static boolean isVirin(String identifier) {
        return US_VIRIN.matcher(identifier).matches();
    }

    public static record VirinTemplates(String virinTemplate, String pdTemplate) {
    }

    public static VirinTemplates getUsVirinTemplates(String virin, URL url) {
        return getUsVirinTemplates(virin, url.toExternalForm());
    }

    public static VirinTemplates getUsVirinTemplates(String virin, String url) {
        Matcher m = US_VIRIN.matcher(virin);
        return m.matches() && !"ZZ999".equals(m.group(3)) ? switch (m.group(2)) {
            case "A" -> new VirinTemplates(vt(virin, "Army", url), "PD-USGov-Military-Army");
            case "D" -> new VirinTemplates(vt(virin, "Department of Defense", url), "PD-USGov-Military");
            case "F" -> new VirinTemplates(vt(virin, "Air Force", url), "PD-USGov-Military-Air Force");
            case "G" -> new VirinTemplates(vt(virin, "Coast Guard", url), "PD-USCG");
            case "H" -> new VirinTemplates(vt(virin, "Department of Homeland Security", url), "PD-USGov-DHS");
            case "M" -> new VirinTemplates(vt(virin, "Marine Corps", url), "PD-USGov-Military-Marines");
            case "N" -> new VirinTemplates(vt(virin, "Navy", url), "PD-USGov-Military-Navy");
            case "O" -> new VirinTemplates(vt(virin, "Other", url), null);
            case "P" -> new VirinTemplates(vt(virin, "Executive Office of the President", url), "PD-USGov-POTUS");
            case "S" -> new VirinTemplates(vt(virin, "Department of State", url), "PD-USGov-DOS");
            case "X" -> new VirinTemplates(vt(virin, "Space Force", url), "PD-USGov-Military-Space Force");
            case "Z" -> new VirinTemplates(vt(virin, "National Guard", url), "PD-USGov-Military-National Guard");
            default -> new VirinTemplates(vt(virin, "Armed Forces", url), null);
        } : null;
    }

    private static String vt(String virin, String organization, String url) {
        return "ID-USMil |1=" + virin + " |2= " + organization + "|url= " + url;
    }

    public static Optional<String> getUsMilitaryCategory(Media media) {
        return ofNullable(switch (media.getId().getRepoId()) {
        case "afspc", "AFSC", "airforcespacecommand" -> "Photographs by the United States Air Force Space Command";
        case "ssc", "SSC", "129133022@N07" -> "Photographs by the Space Systems Command";
        case "jtfsd", "spacecom", "USSPACECOM" -> "Photographs by the United States Space Command";
        case "spoc" ->
            media.getYear().isBefore(Year.of(2020)) ? "Photographs by the United States Air Force Space Command" : null;
        default -> null;
        });
    }

    public static String getUsMilitaryEmoji(Media media) {
        return switch (media.getId().getRepoId()) {
        case "SLD30", "45SW", "patrick", "vandenberg" -> Emojis.ROCKET;
        default -> Emojis.FLAG_USA;
        };
    }

    public static String getUsMilitaryTwitterAccount(Media media) {
        return switch (media.getId().getRepoId()) {
        case "buckley" -> "@Buckley_SFB";
        case "SBD1" -> "@PeteSchriever";
        case "vandenberg", "SLD30" -> "@SLDelta30";
        case "patrick", "45SW" -> "@SLDelta45";
        case "ssc", "SSC", "129133022@N07" -> "@USSF_SSC";
        case "spoc", "SpOC" -> "@ussfspoc";
        case "starcom", "STARCOM" -> "@USSF_STARCOM";
        case "jtfsd", "spacecom", "USSPACECOM" -> "@US_SpaceCom";
        default -> "@SpaceForceDoD";
        };
    }
}
