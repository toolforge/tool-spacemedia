package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble.HubbleNasaImageFiles;

class StsciServiceTest {

    @Test
    void testFileDownloadTextPattern() throws IOException {
        doTest("7400 X 4162, PNG (20.44 MB)", 7400, 4162, 21432893);
        doTest("4684 X 2807, TIF (21.41 MB)", 4684, 2807, 22450012);
        doTest("Text Description, PDF (90.65 KB)", 0, 0, 92825);
    }

    private static void doTest(String text, int width, int height, int sizeInBytes) throws IOException {
        HubbleNasaImageFiles file = StsciService.extractFile(null, null, text);
        assertNotNull(file);
        assertEquals(width, file.getWidth());
        assertEquals(height, file.getHeight());
        assertEquals(sizeInBytes, file.getFileSize());
    }
}
