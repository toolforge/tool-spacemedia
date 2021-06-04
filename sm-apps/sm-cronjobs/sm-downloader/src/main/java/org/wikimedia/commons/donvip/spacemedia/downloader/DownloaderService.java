package org.wikimedia.commons.donvip.spacemedia.downloader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.AudioFile;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.File;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.FileFormat;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.FilePublication;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.ImageFile;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.PdfFile;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.VideoFile;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.repository.FilePublicationRepository;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.repository.FileRepository;

@Service
public class DownloaderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloaderService.class);

    /**
     * Resulting SHA-1 hash of an HTML error page.
     * See <a href="https://www.esa.int/var/esa/storage/images/esa_multimedia/images/2006/10/envisat_sees_madagascar/10084739-2-eng-GB/Envisat_sees_Madagascar.tiff">this example</a>
     */
    private static final String SHA1_ERROR = "860f6466c5f3da5d62b2065c33aa5548697d817c";

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FilePublicationRepository filePublicationRepository;

    @Autowired
    private ImageProcessorService imageProcessorService;

    public void downloadFiles() {
        for (FilePublication fp : filePublicationRepository.findByFileNullOrderByIdId()) {
            URL url = fp.getUrl();
            try {
                Path temp = Files.createTempFile("tmp-", ".file");
                LOGGER.info("Downloading {} ...", url);
                URLConnection connection = url.openConnection();
                try (ReadableByteChannel readableByteChannel = Channels.newChannel(connection.getInputStream());
                        FileOutputStream fileOutputStream = new FileOutputStream(temp.toFile());
                        FileChannel fileChannel = fileOutputStream.getChannel();) {
                    long bytes = fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                    LOGGER.debug("Downloaded {} bytes", bytes);
                    String contentType = connection.getContentType();
                    FileFormat format = FileFormat.fromContentType(contentType);
                    if (format == null) {
                        LOGGER.error("Unknown file format: {}", contentType);
                    }
                    String sha1 = HashHelper.computeSha1(temp);
                    Optional<File> fileInRepo = fileRepository.findById(sha1);
                    File file;
                    if (fileInRepo.isPresent()) {
                        file = fileInRepo.get();
                    } else {
                        file = format.getFileClass().getDeclaredConstructor().newInstance();
                        file.setFormat(format);
                        file.setSha1(sha1);
                        file.setSize(bytes);
                        if (file instanceof ImageFile) {
                            imageProcessorService.processImageFile(((ImageFile) file), temp);
                        } else if (file instanceof VideoFile) {
                            // TODO
                        } else if (file instanceof AudioFile) {
                            // TODO
                        } else if (file instanceof PdfFile) {
                            // TODO
                        }
                        file = fileRepository.save(file);
                    }
                    fp.setFile(file);
                    LOGGER.info("Retrieved file: {}", file);
                    filePublicationRepository.save(fp);
                } catch (IOException | ReflectiveOperationException e) {
                    LOGGER.error("Error while downloading {}", url, e);
                } finally {
                    Files.delete(temp);
                }
            } catch (IOException e) {
                LOGGER.error("Error while creating temp file", e);
            }
        }
    }
}
