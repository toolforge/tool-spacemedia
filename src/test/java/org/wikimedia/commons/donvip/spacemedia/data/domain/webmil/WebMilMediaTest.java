package org.wikimedia.commons.donvip.spacemedia.data.domain.webmil;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;

class WebMilMediaTest {

    @Test
    void testGetUploadTitle() throws Exception {
        WebMilMedia media = new WebMilMedia();
        media.setId(new CompositeMediaId("spoc", "2000684206"));
        media.setVirin("082808-F-9999F-003");
        media.setTitle(
                "retired Maj. Gen. Richard J. O’Lear; Mr. Patrick Ellis, current Air Force TENCAP director; retired General Ronald R. Fogelman, former Air Force Chief of Staff; retired Maj. Gen. Glen W. “Wally” Moorhead III; and Col. Robert F. “Bob” Wright, commander, Spa");
        FileMetadata fm = new FileMetadata(
                "https://media.defense.gov/2008/Aug/26/2000684206/-1/-1/0/082808-F-9999F-003.JPG");
        media.addMetadata(fm);

        String title = media.getUploadTitle(fm) + '.' + fm.getFileExtension();
        assertTrue(title.length() < 240);
        assertTrue(title.getBytes("UTF-8").length < 240);
    }
}
