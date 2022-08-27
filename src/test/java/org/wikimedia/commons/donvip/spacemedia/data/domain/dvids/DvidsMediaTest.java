package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DvidsMediaTest {

    @Test
    void testUploadTitle() {
        DvidsMedia media = new DvidsImage();
        media.setUnit("Space Systems Command");
        DvidsMediaTypedId id = new DvidsMediaTypedId();
        id.setType(DvidsMediaType.image);
        media.setId(id);

        media.setTitle("9/11 2017");
        id.setId(3796505L);
        assertEquals("9/11 2017 (Space Systems Command 3796505)", media.getUploadTitle());

        media.setTitle("AF 70th Birthday at Angel game");
        id.setId(3793475L);
        assertEquals("AF 70th Birthday at Angel game (3793475)", media.getUploadTitle());
    }
}
