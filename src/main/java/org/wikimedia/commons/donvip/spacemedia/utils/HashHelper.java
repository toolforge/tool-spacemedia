package org.wikimedia.commons.donvip.spacemedia.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.github.kilianB.hash.Hash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;

public final class HashHelper {

    private static final int PHASH_RADIX = 36;

    private static final int BIT_RESOLUTION = 256;

    private static final HashingAlgorithm ALGORITHM = new PerceptiveHash(BIT_RESOLUTION);

    private static final int ALGORITHM_ID = ALGORITHM.algorithmId();

    private HashHelper() {
        // Hide default constructor
    }

    public static String computeSha1(BufferedImage image) throws IOException {
        try (InputStream in = Utils.getImageInputStream(image)) {
            return DigestUtils.sha1Hex(in);
        }
    }

    public static String computeSha1(Path localPath) throws IOException {
        try (InputStream in = Files.newInputStream(localPath)) {
            return DigestUtils.sha1Hex(in);
        }
    }

    public static String computeSha1(URL httpUrl) throws IOException, URISyntaxException {
        URI uri = Utils.urlToUri(httpUrl);
        try (CloseableHttpClient httpclient = HttpClients.createDefault();
                CloseableHttpResponse response = httpclient.execute(new HttpGet(uri));
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
            return DigestUtils.sha1Hex(in);
        }
    }

    public static BigInteger computePerceptualHash(BufferedImage image) {
        return ALGORITHM.hash(image).getHashValue();
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
