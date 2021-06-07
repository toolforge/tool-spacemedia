package org.wikimedia.commons.donvip.spacemedia.harvester.dlr.flickr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:application.properties")
class DlrFlickrPropertiesTest {

    @Value("${flickr.credit.regex}")
    private String creditRegex;

    @Test
    void testCreditRegex1() {
        doTest("ESA/DLR/FU Berlin, CC BY-SA 3.0 IGO",
                "Ursache für die auffallenden Hell-Dunkel-Schattierungen, die man auch auf diesem Bild sieht, sind Unterschiede in der Mineralogie. Helle Gebiete sind von Staub bedeckt, der einen größeren Anteil von hellen Mineralen enthält, dazu gehören Silikate, die einen hohen Anteil an Siliziumdioxid (SiO2) enthalten, das Elemente wie Aluminium, Kalzium, Kalium oder Natrium im Kristallgitter an sich bindet. Dunkle Gebiete sind dagegen hauptsächlich von Material bedeckt, das aus ‚mafischen‘, dunklen Mineralen bestehen, wie zum Beispiel den Bestandteilen von Basalt. Sie haben einen deutlich niedrigeren SiO2-Anteil und enthalten größere Mengen an Eisen, Magnesium und Titan. Die Aufnahmen dieser Region mit der HRSC (High Resolution Stereo Camera) entstanden am 11. Dezember 2018 während Orbit 18.904 von Mars Express. Die Bildauflösung beträgt etwa 13 Meter pro Bildpunkt (Pixel). Diese perspektivische Schrägansicht wurde aus den Geländemodell-Daten sowie dem Nadirkanal und den Farbkanälen der HRSC berechnet.\r\n"
                        + "\r\n" + "Mehr dazu:\r\n"
                        + "<a href=\"https://www.dlr.de/dlr/desktopdefault.aspx/tabid-10333/623_read-37100/#/gallery/36219\" rel=\"noreferrer nofollow\">www.dlr.de/dlr/desktopdefault.aspx/tabid-10333/623_read-3...</a>\r\n"
                        + "\r\n" + "Über die Mission Mars Express:\r\n"
                        + "<a href=\"http://www.dlr.de/mars\" rel=\"noreferrer nofollow\">www.dlr.de/mars</a>\r\n"
                        + "\r\n" + "Quelle: ESA/DLR/FU Berlin, CC BY-SA 3.0 IGO");
    }

    @Test
    void testCreditRegex2() {
        doTest("Minikin / DLR (CC-BY 3.0)",
                "Mit der Mission &quot;SOUTHTRAC&quot; (Transport and Composition of the Southern Hemisphere UTLS) erkundet das deutsche Forschungsflugzeug HALO im September und November 2019 die südliche Atmosphäre und ihre Auswirkungen auf den Klimawandel. Das wichtigste Ziel der ersten Kampagnenphase ist die Untersuchung von Schwerewellen an der Südspitze Amerikas und über der Antarktis. In der zweiten Kampagnenphase im November bilden Untersuchungen des Luftmassenaustauschs zwischen Stratosphäre und Troposphäre den wissenschaftlichen Schwerpunkt.\r\n"
                        + "\r\n"
                        + "<a href=\"https://www.dlr.de/dlr/desktopdefault.aspx/tabid-10081/151_read-37646/\" rel=\"noreferrer nofollow\">www.dlr.de/dlr/desktopdefault.aspx/tabid-10081/151_read-37646/</a>\r\n"
                        + "\r\n" + "Credit: Minikin / DLR (CC-BY 3.0)");
    }

    @Test
    void testCreditRegex3() {
        doTest("ESA/DLR/FU Berlin, CC BY-SA 3.0 IGO",
                "Eines der ungewöhnlichsten Bilder, das je vom Mars aufgenommen wurde: Wie Schaumkrönchen auf einer Ozeanwelle rage die mehrere Tausend Meter hohen Berge im Randwall des Einschlagsbeckens Argyre in den Marshimmel. Die HRSC-Kamera an Bord der Mars Express-Raumsonde hat dieses Bild gegen den Marshorizont aber in erster Linie deshalb aufgenommen, weil es die Struktur der Marsatmosphäre sichtbar macht. Deutlich ist darauf zu erkennen, wie sich die untere, dichtere Atmosphäre von der Stratosphäre abhebt. Die Entschlüsselung der Klimageschichte und damit die Entwicklung der Gashülle im Laufe der 4,5 Milliarden Jahre langen Geschichte des Planeten bleibt eines der wichtigsten Themen der Marsforschung.\r\n"
                        + "\r\n" + "Credit: ESA/DLR/FU Berlin, CC BY-SA 3.0 IGO\r\n" + "\r\n"
                        + "Mehr zur Mission Mars Express: <a href=\"http://www.dlr.de/dlr/desktopdefault.aspx/tabid-10333/\" rel=\"nofollow\">www.dlr.de/dlr/desktopdefault.aspx/tabid-10333/</a>\r\n"
                        + "\r\n"
                        + "Special: Der Mars - Ein Planet voller Rätsel: <a href=\"http://www.mex10.dlr.de/\" rel=\"nofollow\">www.mex10.dlr.de/</a>\r\n"
                        + "\r\n" + "");
    }

    private void doTest(String expected, String provided) {
        Matcher matcher = Pattern.compile(creditRegex, Pattern.MULTILINE).matcher(provided);
        assertTrue(matcher.find());
        assertEquals(expected, matcher.group(1));
    }
}
