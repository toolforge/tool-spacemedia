package org.wikimedia.commons.donvip.spacemedia.data.domain.stsci;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StsciMediaTest {

    @Test
    void testGetUploadId() {
        StsciMedia media = new StsciMedia();

        media.setId("1993/22/117-Image");
        assertEquals("1993-22-117", media.getUploadId());

        media.setId("4636-Image");
        assertEquals("4636", media.getUploadId());

        media.setId("2022/036/01G9HYYGC4MGATEMJ88R4HE6QV");
        assertEquals("2022-036", media.getUploadId());
    }
}
