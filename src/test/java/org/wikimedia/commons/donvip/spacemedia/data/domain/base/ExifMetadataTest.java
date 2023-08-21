package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.wikimedia.commons.donvip.spacemedia.utils.ImageUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

class ExifMetadataTest {

    @Test
    void testJsonDeserialisation() throws Exception {
        assertNotNull(new ObjectMapper().readValue(new File("src/test/resources/nasa/P22-003-22-metadata.json"),
                ExifMetadata.class));
    }

    @Test
    void testReadImageMetadata() throws Exception {
        ExifMetadata metadata = ExifMetadata.of(ImageUtils
                .readImageMetadata(new URL("https://images-assets.nasa.gov/image/P22-003-22/P22-003-22~orig.jpg")));
        assertNotNull(metadata);

        assertEquals("Chris Hanoch", metadata.getExifArtist());
        assertEquals("Copyright © 2022 Lockheed Martin Corporation", metadata.getExifCopyright());
        assertEquals("""
                Lockheed Martin Aeronautics Company - Fort Worth - Chris Hanoch
                Subject: X-59 - Various Angles in Test Fixture
                FP#: 21-03420
                POC: Analiese Smith, Chris Higgins
                Other info: X-59 in Fort Worth, testing; high angle shots in fixture 1-10-22""",
                metadata.getExifImageDescription());
        assertEquals(4480, metadata.getExifImageHeight());
        assertEquals(6720, metadata.getExifImageWidth());
        assertEquals("000082f27e", metadata.getExifLensSerialNumber());
        assertEquals("122055003806", metadata.getExifSerialNumber());

        assertEquals("JPEG", metadata.getFileFileType());
        assertEquals("jpg", metadata.getFileFileTypeExtension());
        assertEquals("image/jpeg", metadata.getFileMimeType());

        assertEquals("Chris Hanoch", metadata.getIptcByLine());
        assertEquals("""
                Lockheed Martin Aeronautics Company - Fort Worth - Chris Hanoch
                Subject: X-59 - Various Angles in Test Fixture
                FP#: 21-03420
                POC: Analiese Smith, Chris Higgins
                Other info: X-59 in Fort Worth, testing; high angle shots in fixture 1-10-22""",
                metadata.getIptcCaptionAbstract().replace("\r", "\n"));
        assertEquals("Copyright © 2022 Lockheed Martin Corporation", metadata.getIptcCopyrightNotice());
        assertEquals("X-59 - Various Angles in Test Fixture", metadata.getIptcObjectName());
        assertEquals("Chris Hanoch", metadata.getIptcWriterEditor());

        assertEquals("http://www.lockheedmartin.com/us/contact/licensing-information.html",
                metadata.getPhotoshopUrl());

        assertEquals("Chris Hanoch", metadata.getXmpCaptionWriter());
        assertEquals("""
                Lockheed Martin Aeronautics Company - Fort Worth - Chris Hanoch
                Subject: X-59 - Various Angles in Test Fixture
                FP#: 21-03420
                POC: Analiese Smith, Chris Higgins
                Other info: X-59 in Fort Worth, testing; high angle shots in fixture 1-10-22""",
                metadata.getXmpDescription());
        assertEquals("image/jpeg", metadata.getXmpFormat());
        assertEquals("000082f27e", metadata.getXmpLensSerialNumber());
        assertEquals("AE88EB9CEDFF4AB5B939380DECE88D1E", metadata.getXmpOriginalDocumentID());
        assertEquals("Copyright © 2022 Lockheed Martin Corporation", metadata.getXmpRights());
        assertEquals("122055003806", metadata.getXmpSerialNumber());
        assertEquals("X-59 - Various Angles in Test Fixture", metadata.getXmpTitle());
        assertEquals("http://www.lockheedmartin.com/us/contact/licensing-information.html",
                metadata.getXmpWebStatement());
    }
}
