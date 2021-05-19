package org.wikimedia.commons.donvip.spacemedia.utils;

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
}
