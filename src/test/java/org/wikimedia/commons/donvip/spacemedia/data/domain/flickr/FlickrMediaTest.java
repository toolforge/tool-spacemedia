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

class FlickrMediaTest {

    @Test
    void testGetUploadTitle() {
        FlickrMedia media = new FlickrMedia();
        media.setId(26635829145L);
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

    @ParameterizedTest
    @CsvSource({ "NHQ202305030019,Czech Republic Artemis Accords Signing (NHQ202305030019)",
            "GRC-2023-C-03833,GRC-2023-C-03833" })
    void testGetUserDefinedId(String expectedId, String title) {
        FlickrMedia media = new FlickrMedia();
        media.setTitle(title);
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
}
