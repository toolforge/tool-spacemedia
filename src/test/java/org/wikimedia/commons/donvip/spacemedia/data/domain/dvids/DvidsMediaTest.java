package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;

class DvidsMediaTest {

    @Test
    void testUploadTitle() {
        DvidsMedia media = new DvidsImage();
        media.setUnitName("Space Systems Command");
        CompositeMediaId id = new CompositeMediaId();
        id.setRepoId("SSC");
        media.setId(id);

        media.setTitle("9/11 2017");
        id.setMediaId("image:3796505");
        assertEquals("9-11 2017 (Space Systems Command 3796505)", media.getUploadTitle(null));

        media.setTitle("AF 70th Birthday at Angel game");
        id.setMediaId("image:3793475");
        assertEquals("AF 70th Birthday at Angel game (3793475)", media.getUploadTitle(null));

        media.setTitle(
                "http://www.patrick.af.mil/News/Article-Display/Article/1319994/air-force-senior-leaders-thank-team-patrick-cape-for-irma-recovery-efforts");
        id.setMediaId("image:3809731");
        assertEquals(
                "www_patrick_af_mil-News-Article-Display-Article-1319994-air-force-senior-leaders-thank-team-patrick-cape-for-irma-recovery-efforts (3809731)",
                media.getUploadTitle(null));
    }
}
