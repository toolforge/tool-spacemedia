package org.wikimedia.commons.donvip.spacemedia.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public final class UriUrlUtils {

    private UriUrlUtils() {
        // Hide default constructor
    }

    public static URI urlToUri(URL url) throws URISyntaxException {
        URI uri = null;
        try {
            uri = url.toURI();
        } catch (URISyntaxException e) {
            uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), null);
        }
        return new URI(uri.toASCIIString());
    }

    public static String getContentType(URL url) throws IOException {
        HttpURLConnection.setFollowRedirects(true);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        return connection.getContentType();
    }
}
