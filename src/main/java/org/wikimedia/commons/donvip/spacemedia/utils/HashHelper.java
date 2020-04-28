package org.wikimedia.commons.donvip.spacemedia.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageDecodingException;

import com.github.kilianB.hash.Hash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;

public final class HashHelper {

    private static final int bitResolution = 2 ^ 30;

    private static final HashingAlgorithm algorithm = new PerceptiveHash(bitResolution);

    private static final int algorithmId = algorithm.algorithmId();

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
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException(uri + " => " + response.getStatusLine());
            }
            return DigestUtils.sha1Hex(in);
        }
    }

    public static BigInteger computePerceptualHash(BufferedImage image, URL url)
            throws IOException, URISyntaxException, ImageDecodingException {
        return algorithm.hash(image != null ? image : Utils.readImage(url, false)).getHashValue();
    }

    public static double similarityScore(BigInteger phash1, BigInteger phash2) {
        return newHash(phash1).normalizedHammingDistanceFast(newHash(phash2));
    }

    private static Hash newHash(BigInteger phash) {
        return new Hash(phash, bitResolution, algorithmId);
    }
}
