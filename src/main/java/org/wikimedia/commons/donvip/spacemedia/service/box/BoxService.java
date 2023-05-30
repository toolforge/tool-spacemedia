package org.wikimedia.commons.donvip.spacemedia.service.box;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;

@Service
public class BoxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BoxService.class);

    private static final String REDIRECT_URL = "https://spacemedia.toolforge.org";

    private BoxAPIConnection api;

    public BoxService(
            @Value("${box.api.oauth2.client-id}") String clientId,
            @Value("${box.api.oauth2.client-secret}") String clientSecret,
            @Value("${box.api.user-email}") String userEmail,
            @Value("${box.api.user-password}") String userPassword)
            throws IOException {
        if (isNotBlank(clientId) && isNotBlank(clientSecret) && isNotBlank(userEmail) && isNotBlank(userPassword)) {
            // Access tokens expire quickly, so request new token at startup
            String authURL = BoxAPIConnection.getAuthorizationURL(clientId, URI.create(REDIRECT_URL), "state", null)
                    .toExternalForm();

            try (CloseableHttpClient httpclient = HttpClientBuilder.create()
                    .setRedirectStrategy(new BoxRedirectStrategy()).build()) {
                // STEP 1 - Request login page
                try (CloseableHttpResponse response1 = httpclient.execute(new HttpGet(authURL));
                        InputStream in1 = response1.getEntity().getContent()) {
                    if (response1.getStatusLine().getStatusCode() >= 400) {
                        throw new IOException(response1.getStatusLine().toString());
                    }
                    HttpPost loginPost = httpPostFor(
                            Jsoup.parse(in1, "UTF-8", authURL).getElementsByClass("form login_form").first(),
                            (input, params) -> params.add(new BasicNameValuePair(input.attr("name"),
                                    "login".equals(input.attr("id")) ? userEmail
                                            : "password".equals(input.attr("id")) ? userPassword
                                                    : input.attr("value"))));

                    // STEP 2 - Login and request authorize page
                    try (CloseableHttpResponse response2 = httpclient.execute(loginPost);
                            InputStream in2 = response2.getEntity().getContent()) {
                        if (response2.getStatusLine().getStatusCode() >= 400) {
                            throw new IOException(response2.getStatusLine().toString());
                        }
                        HttpPost authorizePost = httpPostFor(
                                Jsoup.parse(in2, "UTF-8", authURL).getElementById("consent_form"), (input, params) -> {
                                    if (!"consent_reject_button".equals(input.attr("id"))) {
                                        params.add(new BasicNameValuePair(input.attr("name"), input.attr("value")));
                                    }
                                });

                        // STEP 3 - authorize and request authorization code
                        try (CloseableHttpResponse response3 = httpclient.execute(authorizePost)) {
                            if (response3.getStatusLine().getStatusCode() != 302) {
                                throw new IOException(response3.getStatusLine().toString());
                            }
                            // https://app.box.com/developers/console
                            api = new BoxAPIConnection(clientId, clientSecret,
                                    response3.getFirstHeader("location").getValue().split("=")[2]);
                        }
                    }
                }
            }
        } else {
            LOGGER.warn("Incomplete Box credentials configuration => Box API will not be available");
        }
    }

    private static HttpPost httpPostFor(Element form, BiConsumer<Element, List<NameValuePair>> consumer)
            throws IOException {
        if (form == null) {
            throw new IOException("Form not found");
        }
        HttpPost post = new HttpPost(form.attr("action"));
        List<NameValuePair> params = new ArrayList<>();
        form.getElementsByTag("input").forEach(input -> consumer.accept(input, params));
        post.setEntity(new UrlEncodedFormEntity(params));
        return post;
    }

    public List<BoxFile.Info> getFiles(String sharedLink) {
        LOGGER.info("Fetching files of shared folder {}", sharedLink);
        BoxItem.Info itemInfo = BoxItem.getSharedItem(api, sharedLink);
        if (itemInfo instanceof BoxFile.Info fileInfo) {
            return List.of(fileInfo);
        } else if (itemInfo instanceof BoxFolder.Info folderInfo) {
            return getFiles(folderInfo, folderInfo.getName());
        }
        throw new IllegalArgumentException(Objects.toString(itemInfo));
    }

    private static List<BoxFile.Info> getFiles(BoxFolder.Info parentFolderInfo, String path) {
        LOGGER.info("Fetching files of folder {} - {}", parentFolderInfo.getID(), path);
        List<BoxFile.Info> result = new ArrayList<>();
        for (BoxItem.Info itemInfo : parentFolderInfo.getResource()) {
            if (itemInfo instanceof BoxFile.Info fileInfo) {
                result.add(fileInfo);
            } else if (itemInfo instanceof BoxFolder.Info folderInfo && !"__MACOSX".equals(folderInfo.getName())) {
                result.addAll(getFiles(folderInfo, path + " > " + folderInfo.getName()));
            }
        }
        return result;
    }

    private static class BoxRedirectStrategy extends LaxRedirectStrategy {

        @Override
        public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context)
                throws ProtocolException {
            Header locationHeader = response.getFirstHeader("location");
            return locationHeader != null && !locationHeader.getValue().contains(REDIRECT_URL)
                    && super.isRedirected(request, response, context);
        }
    }
}
