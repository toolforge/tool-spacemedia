package org.wikimedia.commons.donvip.spacemedia.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.channels.Channels;

import org.junit.jupiter.api.Test;

class Mp4FileTest {

    @Test
    void testClose() {
        assertDoesNotThrow(
                () -> new Mp4File(Channels.newChannel(MediaUtils.class.getResourceAsStream("/mp4-with-audio.mp4")))
                        .close());
    }
}
