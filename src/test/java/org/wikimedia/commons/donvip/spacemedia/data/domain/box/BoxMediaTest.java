package org.wikimedia.commons.donvip.spacemedia.data.domain.box;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;

class BoxMediaTest {

    @ParameterizedTest
    @CsvSource({ "IMG_1646.JPG,IMG_1646", "50169626668_fc0f94b81a_o.jpg,50169626668_fc0f94b81a_o", "0012.png,0012" })
    void testGetUploadTitle(String title, String expectedTitle) {
        BoxMedia media = new BoxMedia();
        media.setId(new CompositeMediaId("app/share", "1"));
        media.setTitle(title);
        assertEquals("app " + expectedTitle, media.getUploadTitle());
    }
}
