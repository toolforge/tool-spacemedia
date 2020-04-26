package org.wikimedia.commons.donvip.spacemedia.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageDecodingException;

import com.github.kilianB.hashAlgorithms.PerceptiveHash;

public final class HashHelper {

    private HashHelper() {
        // Hide default constructor
    }

    public static String computeSha1(BufferedImage image) throws IOException {
        try (InputStream in = Utils.getImageInputStream(image)) {
            return DigestUtils.sha1Hex(in);
        }
    }

    public static String computeSha1(URL url) throws IOException, URISyntaxException {
        URI uri = Utils.urlToUri(url);
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
        return new PerceptiveHash(2 ^ 16).hash(image != null ? image : Utils.readImage(url, false)).getHashValue();
    }
}
