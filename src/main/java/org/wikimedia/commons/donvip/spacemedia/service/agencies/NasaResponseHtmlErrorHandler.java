package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaResponse;

public class NasaResponseHtmlErrorHandler implements HttpMessageConverter<NasaResponse> {

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return NasaResponse.class.equals(clazz) && MediaType.TEXT_HTML.equals(mediaType);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return List.of(MediaType.TEXT_HTML);
    }

    @Override
    public NasaResponse read(Class<? extends NasaResponse> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        List<String> encodings = inputMessage.getHeaders().get(HttpHeaders.CONTENT_ENCODING);
        try (InputStream in = inputMessage.getBody()) {
            throw new IOException(
                    "HTML contents received instead of JSON: " + new String(in.readAllBytes(),
                            isEmpty(encodings) ? StandardCharsets.UTF_8.name() : encodings.get(0)));
        }
    }

    @Override
    public void write(NasaResponse t, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        // Do nothing
    }
}
