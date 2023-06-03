package org.wikimedia.commons.donvip.spacemedia.utils;

import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newHttpGet;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;
import dev.brachtendorf.jimagehash.hashAlgorithms.PerceptiveHash;

public final class HashHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(HashHelper.class);

    private static final int PHASH_RADIX = 36;

    private static final int BIT_RESOLUTION = 256;

    private static final HashingAlgorithm ALGORITHM = new PerceptiveHash(BIT_RESOLUTION);

    private static final int ALGORITHM_ID = ALGORITHM.algorithmId();

    private HashHelper() {
        // Hide default constructor
    }

    public static String computeSha1(Path localPath) throws IOException {
        try (InputStream in = Files.newInputStream(localPath)) {
            return DigestUtils.sha1Hex(in);
        }
    }

    public static String computeSha1(URL httpUrl) throws IOException {
        URI uri = Utils.urlToUriUnchecked(httpUrl);
        try (CloseableHttpClient httpclient = HttpClients.createDefault();
                CloseableHttpResponse response = httpclient.execute(newHttpGet(uri));
                InputStream in = response.getEntity().getContent()) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                org.springframework.http.HttpStatus status = org.springframework.http.HttpStatus.valueOf(statusCode);
                String statusText = response.getStatusLine().toString();
                if (statusCode >= 500) {
                    throw HttpServerErrorException.create(status, statusText, null, null, StandardCharsets.UTF_8);
                } else if (statusCode >= 400) {
                    throw HttpClientErrorException.create(status, statusText, null, null, StandardCharsets.UTF_8);
                }
                throw new IOException(uri + " => " + statusText);
            }
            String result = DigestUtils.sha1Hex(in);
            LOGGER.debug("SHA1 for {} => {}", uri, result);
            return result;
        }
    }

    /**
     * Computes the perceptual hash of the given image
     *
     * @param image buferred image
     * @return perceptual hash
     * @throws NullPointerException deep into JDK for some images (at
     *                              java.desktop/java.awt.image.ComponentColorModel.getDataElements(ComponentColorModel.java:1557))
     */
    public static BigInteger computePerceptualHash(BufferedImage image) {
        return ALGORITHM.hash(image).getHashValue();
    }

    public static double similarityScore(String phash1, String phash2) {
        return similarityScore(decode(phash1), decode(phash2));
    }

    public static double similarityScore(BigInteger phash1, String phash2) {
        return similarityScore(phash1, decode(phash2));
    }

    public static double similarityScore(BigInteger phash1, BigInteger phash2) {
        return newHash(phash1).normalizedHammingDistanceFast(newHash(phash2));
    }

    private static Hash newHash(BigInteger phash) {
        return new Hash(phash, BIT_RESOLUTION, ALGORITHM_ID);
    }

    public static BigInteger decode(String phash) {
        return phash != null ? new BigInteger(phash, PHASH_RADIX) : null;
    }

    public static String encode(BigInteger phash) {
        return phash != null ? phash.toString(PHASH_RADIX) : null;
    }
}
