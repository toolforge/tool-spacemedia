package org.wikimedia.commons.donvip.spacemedia.data.domain.flickr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;

class FlickrMediaTest {

    @Test
    void testGetUploadTitle1() {
        FlickrMedia media = new FlickrMedia();
        media.setId(new CompositeMediaId("", "26635829145"));
        FlickrPhotoSet photoset = new FlickrPhotoSet();
        photoset.setTitle("Flugversuche mit dem A320 ATRA");
        media.addPhotoSet(photoset);
        media.setTitle("");
        media.setDescription(
                "Wissenschaftler des Deutschen Zentrums für Luft- und Raumfahrt (DLR) erproben bei Flugversuchen mit dem Airbus A320 D-ATRA (Advanced Technology Research Aircraft) in Braunschweig neue automatische Landeverfahren und satellitengestützte Positionsbestimmungen. Gekrümmte Anflüge und hochpräzise Landungen können Lärm reduzieren und die Umwelt schonen. Die Forscher untersuchten speziell die Kombination von satellitengestützten und traditionellen Landehilfen sowie Verfahren, die allein auf Satellitennavigation beruhen.\r\n"
                        + "\r\n"
                        + "Vollständiger Artikel unter <a href=\"http://www.dlr.de/dlr/desktopdefault.aspx/tabid-10081/151_read-17505/\" rel=\"nofollow\">www.dlr.de/dlr/desktopdefault.aspx/tabid-10081/151_read-17505/</a>\r\n"
                        + "\r\n" + "Fotos: DLR / Maasewerd CC-BY 3.0");
        assertEquals("Flugversuche mit dem A320 ATRA (26635829145)", media.getUploadTitle(null));
    }

    @Test
    void testGetUploadTitle2() {
        FlickrMedia media = new FlickrMedia();
        media.setId(new CompositeMediaId("", "5727383147"));
        FlickrPhotoSet photoset = new FlickrPhotoSet();
        photoset.setTitle("NEEMO 15 Eng Eval Day 4");
        media.addPhotoSet(photoset);
        media.setTitle("GOPR0146");
        media.setDescription("Dcim\\100gopro");
        assertEquals("NEEMO 15 Eng Eval Day 4 (5727383147)", media.getUploadTitle(null));
    }

    @Test
    void testGetUploadTitle3() {
        FlickrMedia media = new FlickrMedia();
        media.setId(new CompositeMediaId("whitehouse", "53912725338"));
        FlickrPhotoSet photoset = new FlickrPhotoSet();
        photoset.setTitle("Winter 2024");
        media.addPhotoSet(photoset);
        media.setTitle("S20240327CS-0772");
        media.setDescription("Second Gentleman Doug Emhoff greets professional pickleball players at the Miami Open Tennis Tournament, Wednesday, March 27, 2024, at Hard Rock Stadium in Miami Gardens, Florida. (Official White House Photo by Cameron Smith)");
        assertEquals("Winter 2024 (S20240327CS-0772)", media.getUploadTitle(null));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "NHQ202305030019;Czech Republic Artemis Accords Signing (NHQ202305030019);xxx",
            "GRC-2023-C-03833;GRC-2023-C-03833;xxx",
            "iss071e406326;The English Channel and the North Sea;iss071e406326 (July 28, 2024) --- The English Channel and the North Sea separate the island of Great Britain from the northwest European nations of The Netherlands, Belgium, and France in this photograph from the International Space Station as it orbited 258 miles above." })
    void testGetUserDefinedId(String expectedId, String title, String description) {
        FlickrMedia media = new FlickrMedia();
        media.setTitle(title);
        media.setDescription(description);
        assertEquals(Optional.of(expectedId), media.getUserDefinedId());
    }

    @Test
    void testAreSameUris() throws URISyntaxException {
        FlickrMedia media = new FlickrMedia();
        assertTrue(media.areSameUris(new URI("https://live.staticflickr.com/2481/3578880072_1f2efcf80c_o.jpg"),
                new URI("https://farm3.staticflickr.com/2481/3578880072_1f2efcf80c_o.jpg")));
        assertTrue(media.areSameUris(new URI("https://farm4.staticflickr.com/2481/3578880072_1f2efcf80c_o.jpg"),
                new URI("https://farm3.staticflickr.com/2481/3578880072_1f2efcf80c_o.jpg")));
        assertFalse(media.areSameUris(new URI("https://live.staticflickr.com/2481/3578880072_1f2efcf80c_o.jpg"),
                new URI("https://live.staticflickr.com/2481/3578880072_1f2efcf80c_m.jpg")));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
        "true;20221130103217_JQM49353",
        "true;20220216095511_IMG_6593",
        "true;20211206113838_CR3A0863"})
    void testIsWrongTitle(boolean expected, String title) {
        assertEquals(expected, new FlickrMedia().isWrongtitle(title));
    }
}
