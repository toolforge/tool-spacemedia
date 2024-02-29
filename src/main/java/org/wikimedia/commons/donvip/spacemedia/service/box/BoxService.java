package org.wikimedia.commons.donvip.spacemedia.service.box;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.checkResponse;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newHttpGet;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newHttpPost;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;

import javax.annotation.PostConstruct;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.box.sdk.SharedLinkAPIConnection;

import okhttp3.OkHttpClient;

@Lazy
@Service
public class BoxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BoxService.class);

    private static final String REDIRECT_URL = "https://spacemedia.toolforge.org";

    private static final Set<String> IGNORED_FOLDERS = Set.of("__MACOSX");

    @Autowired
    private CommonsService commonsService;

    private BoxAPIConnection api;

    public BoxService(
            @Value("${box.api.oauth2.client-id}") String clientId,
            @Value("${box.api.oauth2.client-secret}") String clientSecret,
            @Value("${box.api.user-email}") String userEmail,
            @Value("${box.api.user-password}") String userPassword) {
        if (isNotBlank(clientId) && isNotBlank(clientSecret) && isNotBlank(userEmail) && isNotBlank(userPassword)) {
            // Access tokens expire quickly, so request new token at startup
            String authURL = BoxAPIConnection.getAuthorizationURL(clientId, URI.create(REDIRECT_URL), "state", null)
                    .toExternalForm();

            try (CloseableHttpClient httpclient = HttpClientBuilder.create()
                    .setRedirectStrategy(new BoxRedirectStrategy()).build()) {
                // STEP 1 - Request login page
                HttpRequestBase request = newHttpGet(authURL);
                try (CloseableHttpResponse response1 = checkResponse(request, httpclient.execute(request));
                        InputStream in1 = response1.getEntity().getContent()) {
                    request = newHttpPost(
                            Jsoup.parse(in1, "UTF-8", authURL).getElementsByClass("form login_form").first(),
                            (input, params) -> params.add(new BasicNameValuePair(input.attr("name"),
                                    "login".equals(input.attr("id")) ? userEmail
                                            : "password".equals(input.attr("id")) ? userPassword
                                                    : input.attr("value"))));

                    // STEP 2 - Login and request authorize page
                    try (CloseableHttpResponse response2 = checkResponse(request, httpclient.execute(request));
                            InputStream in2 = response2.getEntity().getContent()) {
                        request = newHttpPost(
                                Jsoup.parse(in2, "UTF-8", authURL).getElementById("consent_form"), (input, params) -> {
                                    if (!"consent_reject_button".equals(input.attr("id"))) {
                                        params.add(new BasicNameValuePair(input.attr("name"), input.attr("value")));
                                    }
                                });

                        // STEP 3 - authorize and request authorization code
                        try (CloseableHttpResponse response3 = checkResponse(request, httpclient.execute(request))) {
                            // https://app.box.com/developers/console
                            api = new BoxAPIConnection(clientId, clientSecret,
                                    response3.getFirstHeader("location").getValue().split("=")[2]);
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to instantiate Box API: {}", e.getMessage(), e);
            }
        } else {
            LOGGER.warn("Incomplete Box credentials configuration => Box API will not be available");
        }
    }

    @PostConstruct
    public void postConstruct() {
        if (LOGGER.isDebugEnabled()) {
            java.util.logging.Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINER);
        } else if (LOGGER.isTraceEnabled()) {
            java.util.logging.Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINEST);
        }
    }

    public String getSharedLink(String app, String share) {
        return "https://" + app + ".app.box.com/s/" + share;
    }

    public String getSharedLink(String app, String share, BoxItem.Info itemInfo) {
        return getSharedLink(app, share, itemInfo.getType(), Long.parseLong(itemInfo.getID()));
    }

    public String getSharedLink(String app, String share, String type, long id) {
        return getSharedLink(app, share) + '/' + type + '/' + id;
    }

    public String getThumbnailUrl(String app, long fileVersion, String share) {
        return "https://" + app + ".app.box.com/representation/file_version_" + fileVersion
                + "/thumb_1024.jpg?shared_name=" + share;
    }

    public BoxItem.Info getSharedItem(URL sharedLink) {
        return getSharedItem(sharedLink.toExternalForm());
    }

    public BoxItem.Info getSharedItem(String sharedLink) {
        return BoxItem.getSharedItem(api, sharedLink);
    }

    public BoxFile getSharedFile(String sharedLink, String fileId) {
        return new BoxFile(new SharedLinkAPIConnection(api, sharedLink), fileId);
    }

    public List<BoxFile.Info> getFiles(String app, String share) {
        return getFiles(app, share, Function.identity(), Comparator.comparing(BoxFile.Info::getCreatedAt));
    }

    public <T> List<T> getFiles(String app, String share, Function<BoxFile.Info, T> mapper, Comparator<T> comparator) {
        String sharedLink = getSharedLink(app, share);
        LOGGER.info("Fetching files of shared folder {}", sharedLink);
        BoxItem.Info itemInfo = getSharedItem(sharedLink);
        if (itemInfo instanceof BoxFolder.Info folderInfo) {
            return getFiles(folderInfo, folderInfo.getName(), mapper).stream().sorted(comparator.reversed()).toList();
        } else if (itemInfo instanceof BoxFile.Info fileInfo && isWantedFile(fileInfo)) {
            return List.of(mapper.apply(fileInfo));
        }
        return List.of();
    }

    private <T> List<T> getFiles(BoxFolder.Info parentFolderInfo, String path,
            Function<BoxFile.Info, T> mapper) {
        LOGGER.info("Fetching files of folder {} - {}", parentFolderInfo.getID(), path);
        List<T> result = new ArrayList<>();
        for (BoxItem.Info itemInfo : parentFolderInfo.getResource().getChildren(BoxItem.ALL_FIELDS)) {
            if (itemInfo instanceof BoxFolder.Info folderInfo
                    && !IGNORED_FOLDERS.contains(folderInfo.getName())) {
                result.addAll(getFiles(folderInfo, path + " > " + folderInfo.getName(), mapper));
            } else if (itemInfo instanceof BoxFile.Info fileInfo && isWantedFile(fileInfo)) {
                result.add(mapper.apply(fileInfo));
            }
        }
        return result;
    }

    private boolean isWantedFile(BoxFile.Info fileInfo) {
        return fileInfo.getSize() > 0 && !fileInfo.getName().startsWith("._") && ("mp4".equals(fileInfo.getExtension())
                || commonsService.isPermittedFileExt(fileInfo.getExtension()));
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
