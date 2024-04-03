package org.wikimedia.commons.donvip.spacemedia.utils;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newHttpGet;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikimedia.commons.donvip.spacemedia.exception.FileDecodingException;

public class MediaUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaUtils.class);

    private static final Set<String> IMAGEIO_EXTENSIONS = Set.of("bmp", "gif", "jpe", "jpg", "jpeg", "png", "svg",
            "tif", "tiff");

    private static final Set<String> POI_HSLF_EXTENSIONS = Set.of("ppt", "pptm");
    private static final Set<String> POI_XSLF_EXTENSIONS = Set.of("pptx");

    private MediaUtils() {
        // Hide default constructor
    }

    public static <T> ContentsAndMetadata<T> readFile(URL url, boolean readMetadata, boolean log)
            throws IOException, FileDecodingException {
        return readFile(Utils.urlToUriUnchecked(url), readMetadata, log);
    }

    @SuppressWarnings("unchecked")
    public static <T> ContentsAndMetadata<T> readFile(URI uri, boolean readMetadata, boolean log)
            throws IOException, FileDecodingException {
        if (log) {
            LOGGER.info("Reading file {}", uri);
        }
        String extension = Utils.findExtension(uri.toString());
        try (CloseableHttpClient httpclient = HttpClients.createDefault();
                CloseableHttpResponse response = httpclient.execute(newHttpGet(uri));
                InputStream in = response.getEntity().getContent()) {
            boolean imageio = extension != null && IMAGEIO_EXTENSIONS.contains(extension);
            String filename = null;
            if (!imageio) {
                Header[] disposition = response.getHeaders("Content-Disposition");
                if (ArrayUtils.isNotEmpty(disposition)) {
                    String value = disposition[0].getValue();
                    if (value.startsWith("attachment;filename=")) {
                        filename = URLDecoder.decode(value.split("=")[1], "UTF-8").replace(";filename*", "")
                                .replace("\"", "");
                    }
                    extension = Utils.findExtension(value);
                    imageio = extension != null && IMAGEIO_EXTENSIONS.contains(extension);
                }
            }
            long contentLength = response.getEntity().getContentLength();
            if (imageio || isBlank(extension)) {
                ContentsAndMetadata<BufferedImage> result = ImageUtils.readImage(in, readMetadata);
                return (ContentsAndMetadata<T>) new ContentsAndMetadata<>(result.contents(), contentLength, filename,
                        isBlank(extension) ? result.extension() : extension, result.numImagesOrPages());
            } else if ("webp".equals(extension)) {
                return (ContentsAndMetadata<T>) new ContentsAndMetadata<>(
                        ImageUtils.readWebp(uri, readMetadata).contents(), contentLength,
                        filename, extension, 1);
            } else if ("pdf".equals(extension)) {
                PDDocument pdf = Loader.loadPDF(new RandomAccessReadBuffer(in));
                return (ContentsAndMetadata<T>) new ContentsAndMetadata<>(pdf, contentLength, filename, extension,
                        pdf.getNumberOfPages());
            } else if (POI_HSLF_EXTENSIONS.contains(extension)) {
                HSLFSlideShow ppt = new HSLFSlideShow(in);
                return (ContentsAndMetadata<T>) new ContentsAndMetadata<>(ppt, contentLength, filename, extension,
                        ppt.getSlides().size());
            } else if (POI_XSLF_EXTENSIONS.contains(extension)) {
                XMLSlideShow ppt = new XMLSlideShow(in);
                return (ContentsAndMetadata<T>) new ContentsAndMetadata<>(ppt, contentLength, filename, extension,
                        ppt.getSlides().size());
            } else {
                throw new FileDecodingException(
                        "Unsupported format: " + extension + " / headers:" + Arrays.stream(response.getAllHeaders())
                                .map(h -> h.getName() + ": " + h.getValue()).sorted().toList());
            }
        }
    }
}
