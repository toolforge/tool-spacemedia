package org.wikimedia.commons.donvip.spacemedia.service.wikimedia;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.httpclient.multipart.BodyPartPayload;
import com.github.scribejava.core.httpclient.multipart.FileByteArrayBodyPartPayload;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;

@Lazy
@Service
public class OAuthHttpService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthHttpService.class);

    @Autowired
    private ObjectMapper jackson;

    public <T> T httpGet(OAuth10aService oAuthService, OAuth1AccessToken oAuthAccessToken, String url,
            Class<T> responseClass) throws IOException {
        return httpCall(oAuthService, oAuthAccessToken, Verb.GET, url, responseClass, Map.of(), Map.of(), null, true);
    }

    public <T> T httpPost(OAuth10aService oAuthService, OAuth1AccessToken oAuthAccessToken, String url,
            Class<T> responseClass, Map<String, String> params, String contentType,
            BodyPartPayload bodyPartPayload) throws IOException {
        return httpCall(oAuthService, oAuthAccessToken, Verb.POST, url, responseClass,
                contentType != null ? Map.of("Content-Type", contentType) : Map.of(), params, bodyPartPayload, true);
    }

    private <T> T httpCall(OAuth10aService oAuthService, OAuth1AccessToken oAuthAccessToken, Verb verb, String url,
            Class<T> responseClass, Map<String, String> headers, Map<String, String> params,
            BodyPartPayload bodyPartPayload, boolean retryOnTimeout) throws IOException {
        LOGGER.debug("{} {} {}", verb, url, params);
        OAuthRequest request = new OAuthRequest(verb, url);
        request.setCharset(UTF_8.name());
        headers.forEach(request::addHeader);
        if (bodyPartPayload != null) {
            request.initMultipartPayload();
            params.forEach((k, v) -> request
                    .addBodyPartPayloadInMultipartPayload(new FileByteArrayBodyPartPayload(v.getBytes(), k)));
            request.addBodyPartPayloadInMultipartPayload(bodyPartPayload);
        } else {
            params.forEach(request::addParameter);
        }
        oAuthService.signRequest(oAuthAccessToken, request);
        try {
            String body = oAuthService.execute(request).getBody();
            if ("upstream request timeout".equalsIgnoreCase(body)) {
                if (retryOnTimeout) {
                    return httpCall(oAuthService, oAuthAccessToken, verb, url, responseClass, headers, params,
                            bodyPartPayload, false);
                } else {
                    throw new IOException(body);
                }
            } else if (body.startsWith("<!DOCTYPE html>")) {
                throw new IOException(body);
            }
            return jackson.readValue(body, responseClass);
        } catch (SocketTimeoutException e) {
            if (retryOnTimeout) {
                return httpCall(oAuthService, oAuthAccessToken, verb, url, responseClass, headers, params,
                        bodyPartPayload, false);
            } else {
                throw e;
            }
        } catch (ExecutionException e) {
            throw new IOException(e);
        } catch (InterruptedException e) {
            GlitchTip.capture(e);
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }
}
