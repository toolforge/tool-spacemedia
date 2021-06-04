package org.wikimedia.commons.donvip.spacemedia.downloader;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.ImageFile;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.repository.MetadataRepository;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

@Service
public class ImageProcessorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageProcessorService.class);

    @Autowired
    private MetadataRepository metadataRepository;

    public void processImageFile(ImageFile imageFile, Path temp) throws IOException {
        processBufferedImage(imageFile, readImage(temp));
        try {
            processMetadata(imageFile, ImageMetadataReader.readMetadata(temp.toFile()));
        } catch (ImageProcessingException | IOException e) {
            LOGGER.warn("Unable to read metadata", e);
        }
    }

    protected static String computePerceptualHash(BufferedImage bufferedImage) {
        return HashHelper.encode(HashHelper.computePerceptualHash(bufferedImage));
    }

    protected BufferedImage readImage(Path temp) throws IOException, IIOException {
        BufferedImage image;
        try (ImageInputStream in = ImageIO.createImageInputStream(Files.newInputStream(temp))) {
            try {
                image = ImageHelper.readImage(in, true);
            } catch (IIOException e) {
                if ("I/O error reading image metadata!".equals(e.getMessage())) {
                    LOGGER.error(e.getMessage(), e);
                    // Error seen with:
                    // https://www.esa.int/var/esa/storage/images/esa_multimedia/images/2005/04/vancouver_and_seattle_seen_by_envisat/10073149-2-eng-GB/Vancouver_and_Seattle_seen_by_Envisat.tif
                    // -
                    // java.io.EOFException: null
                    // at javax.imageio.stream.ImageInputStreamImpl.readShort(ImageInputStreamImpl.java:229)
                    // at javax.imageio.stream.ImageInputStreamImpl.readUnsignedShort(ImageInputStreamImpl.java:242)
                    // at com.sun.imageio.plugins.tiff.TIFFIFD.initialize(TIFFIFD.java:935)
                    // at com.sun.imageio.plugins.tiff.TIFFIFD.initialize(TIFFIFD.java:1069)
                    // at com.sun.imageio.plugins.tiff.TIFFImageMetadata.initializeFromStream(TIFFImageMetadata.java:88)
                    // at com.sun.imageio.plugins.tiff.TIFFImageReader.readMetadata(TIFFImageReader.java:322)
                    LOGGER.info("Reading image again, without metadata");
                    try (ImageInputStream in2 = ImageIO.createImageInputStream(Files.newInputStream(temp))) {
                        image = ImageHelper.readImage(in2, false);
                    }
                } else {
                    throw e;
                }
            }
        }
        if (image == null) {
            throw new IOException("Unable to read image");
        }
        return image;
    }

    protected void processBufferedImage(ImageFile imageFile, BufferedImage image) {
        imageFile.setHeight(image.getHeight());
        imageFile.setWidth(image.getWidth());
        imageFile.setPhash(computePerceptualHash(image));
    }

    private void processMetadata(ImageFile imageFile, Metadata metadata) {
        for (Directory dir : metadata.getDirectories()) {
            for (Tag tag : dir.getTags()) {
                int tagType = tag.getTagType();
                String value = dir.getObject(tagType).toString();
                if (value.length() == 11 && value.charAt(0) == '[' && value.charAt(2) == '@') {
                    value = tag.getDescription();
                }
                if (value != null) {
                    imageFile.addMetadata(metadataRepository.findOrCreate(dir.getName(), tag.getTagName(), value));
                } else {
                    LOGGER.warn("Metadata without textual value: {}", tag);
                }
            }
        }
    }
}
