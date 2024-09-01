package org.wikimedia.commons.donvip.spacemedia.utils;

import static java.util.Locale.ENGLISH;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.ES;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.PT_BR;

import java.net.URL;
import java.time.LocalDate;
import java.time.Year;
import java.util.Optional;
import java.util.function.Function;
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

    private static final Pattern WHITE_HOUSE = Pattern.compile(
            "[FPSV]\\d{8}[A-Z]{2}-\\d{4}");

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

    public static boolean isWhiteHouse(String identifier) {
        return WHITE_HOUSE.matcher(identifier).matches();
    }

    public static record VirinTemplates(String virinTemplate, String pdTemplate, String videoCategory) {
    }

    public static VirinTemplates getUsVirinTemplates(String virin, URL url) {
        return getUsVirinTemplates(virin, url.toExternalForm());
    }

    public static VirinTemplates getUsVirinTemplates(String virin, String url) {
        if (isBlank(virin)) {
            return null;
        }
        Matcher m = US_VIRIN.matcher(virin);
        return m.matches() && !"ZZ999".equals(m.group(3)) ? switch (m.group(2)) {
            case "A" -> new VirinTemplates(vt(virin, "Army", url), "PD-USGov-Military-Army", vd("Army"));
            case "D" -> new VirinTemplates(vt(virin, "Department of Defense", url), "PD-USGov-Military", "Videos from Defense.Gov");
            case "F" -> new VirinTemplates(vt(virin, "Air Force", url), "PD-USGov-Military-Air Force", vd("Air Force"));
            case "G" -> new VirinTemplates(vt(virin, "Coast Guard", url), "PD-USCG", vd("Coast Guard"));
            case "H" -> new VirinTemplates(vt(virin, "Department of Homeland Security", url), "PD-USGov-DHS", vd("Department of Homeland Security"));
            case "M" -> new VirinTemplates(vt(virin, "Marine Corps", url), "PD-USGov-Military-Marines", vd("Marine Corps"));
            case "N" -> new VirinTemplates(vt(virin, "Navy", url), "PD-USGov-Military-Navy", vd("Navy"));
            case "O" -> new VirinTemplates(vt(virin, "Other", url), null, null);
            case "P" -> new VirinTemplates(vt(virin, "Executive Office of the President", url), "PD-USGov-POTUS", "White House videos");
            case "S" -> new VirinTemplates(vt(virin, "Department of State", url), "PD-USGov-DOS", vd("State Department"));
            case "X" -> new VirinTemplates(vt(virin, "Space Force", url), "PD-USGov-Military-Space Force", vd("Space Force"));
            case "Z" -> new VirinTemplates(vt(virin, "National Guard", url), "PD-USGov-Military-National Guard", vd("National Guard"));
            default -> new VirinTemplates(vt(virin, "Armed Forces", url), null, vd("Armed Forces"));
        } : null;
    }

    private static String vt(String virin, String organization, String url) {
        return "ID-USMil |1=" + virin + " |2= " + organization + "|url= " + url;
    }

    private static String vd(String organization) {
        return "Videos of the United States " + organization;
    }

    public static Optional<String> getUsEmbassyCategory(Media media) {
        return Optional.of("Files from the " + switch (media.getId().getRepoId()) {
        case "101399499@N08" -> "U.S. Embassy in South Sudan";
        case "156788110@N04" -> "U.S. Embassy in Ljubljana";
        case "196062858@N03" -> "U.S. Mission to the Dutch Caribbean";
        case "40236643@N04" -> "U.S. Embassy in San Salvador";
        case "89616529@N03" -> "U.S. Embassy in Dhaka";
        case "92297346@N03" -> "U.S. Embassy in Tokyo";
        case "embaixadaeua-brasil" -> "U.S. Embassy in Brasilia";
        case "embaixadaeua-caboverde" -> "U.S. Embassy in Praia";
        case "us-embassy-tashkent" -> "U.S. Embassy in Tashkent";
        default -> throw new IllegalStateException(media.getId().getRepoId());
        } + " Flickr Stream");
    }

    public static Optional<String> getUsEmbassyCreator(Media media) {
        return Optional.of(switch (media.getId().getRepoId()) {
            case "101399499@N08" -> "Q4116404";
            case "156788110@N04" -> "Q67115259";
            case "196062858@N03" -> "Q5164571";
            case "40236643@N04" -> "Q16935247";
            case "89616529@N03" -> "Q19891423";
            case "92297346@N03" -> "Q2331721";
            case "embaixadaeua-brasil" -> "Q10272292";
            case "embaixadaeua-caboverde" -> "Q104759613";
            case "us-embassy-tashkent" -> "Q105635753";
            default -> throw new IllegalStateException(media.getId().getRepoId());
        });
    }

    public static <M extends Media> String getUsEmbassyLanguage(M media, Function<M, String> def) {
        return switch(media.getId().getRepoId()) {
            case "40236643@N04" -> ES;
            case "embaixadaeua-brasil" -> PT_BR;
            default -> def.apply(media);
        };
    }

    public static Optional<String> getUsGovernmentCategory(Media media) {
        return ofNullable(switch (media.getId().getRepoId()) {
            case "whitehouse","whitehouse45","obamawhitehouse" -> media.getCreationDate().isAfter(LocalDate.of(2021, 1, 20))
                    ? "Photographs from the White House during the Biden administration"
                    : media.getCreationDate().isAfter(LocalDate.of(2017, 1, 20))
                            ? "Photographs from the White House during the Trump administration"
                            : media.getCreationDate().isAfter(LocalDate.of(2009, 1, 20))
                                    ? "Photographs from the White House during the Obama administration"
                                    : "Photographs from the White House";
            case "statephotos" -> "Photographs by the U.S. Department of State";
            default -> null;
        });
    }

    public static Optional<String> getUsGovernmentCreator(Media media) {
        return Optional.of(switch (media.getId().getRepoId()) {
            case "whitehouse", "whitehouse45", "obamawhitehouse" -> "Q1355327";
            case "statephotos" -> "Q789915";
            default -> throw new IllegalStateException(media.getId().getRepoId());
        });
    }

    public static String getUsGovernmentLicence(Media media) {
        return switch (media.getId().getRepoId()) {
            case "whitehouse", "whitehouse45", "obamawhitehouse" -> "PD-USGov-POTUS";
            case "statephotos" -> "PD-USGov-DOS";
            default -> throw new IllegalStateException(media.getId().getRepoId());
        };
    }

    public static Optional<String> getUsMilitaryCategory(Media media) {
        return ofNullable(switch (media.getId().getRepoId().toLowerCase(ENGLISH)) {
        case "afspc", "afsc", "airforcespacecommand" -> "Photographs by the United States Air Force Space Command";
        case "ssc", "129133022@n07" -> "Photographs by the Space Systems Command";
        case "jtfsd", "spacecom", "usspacecom" -> "Photographs by the United States Space Command";
        case "spoc" ->
            media.getYear().isBefore(Year.of(2020)) ? "Photographs by the United States Air Force Space Command" : null;
        default -> null;
        });
    }

    public static Optional<String> getUsMilitaryCreator(Media media) {
        return Optional.ofNullable(switch (media.getId().getRepoId().toLowerCase(ENGLISH)) {
            // TODO store these values in Wikidata thanks to a new property
            case "afspc", "afsc", "airforcespacecommand" -> "Q407203";
            case "ssc", "129133022@n07" -> "Q2306400";
            case "jtfsd", "spacecom", "usspacecom" -> "Q7892209";
            case "spoc" -> media.getYear().isBefore(Year.of(2020)) ? "Q407203" : "Q80815922";
            case "starcom" -> "Q108226200";
            case "21sw" -> "Q4631162";
            case "310sw" -> "Q4634679";
            case "45sw" -> "Q4638258";
            case "460sw-pa" -> "Q16207108";
            case "50sw" -> "Q609146";
            case "b-gar" -> "Q97671318";
            case "sbd1" -> "Q104869543";
            case "sld30" -> "Q4634644";
            case "ussf-pa" -> "Q55088961";
            case "207-mi-bde" -> "Q55602691";
            case "usmcfe" -> "Q2495381";
            default -> null;
        });
    }

    public static String getUsMilitaryEmoji(Media media) {
        return switch (media.getId().getRepoId().toLowerCase(ENGLISH)) {
        case "sld30", "45sw", "patrick", "vandenberg" -> Emojis.ROCKET;
        default -> Emojis.FLAG_USA;
        };
    }
}
