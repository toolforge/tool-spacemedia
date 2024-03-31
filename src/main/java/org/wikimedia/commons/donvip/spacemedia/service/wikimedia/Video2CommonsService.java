package org.wikimedia.commons.donvip.spacemedia.service.wikimedia;

import static java.util.Objects.requireNonNull;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.checkResponse;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newHttpGet;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newHttpPost;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Map;

import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Video2CommonsTask;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Video2CommonsTaskRepository;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Lazy
@Service
public class Video2CommonsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Video2CommonsService.class);

    private static final String URL_BASE = "https://video2commons.toolforge.org/";
    private static final String URL_API = URL_BASE + "api";

    @Autowired
    private ObjectMapper jackson;

    @Autowired
    private Video2CommonsTaskRepository repository;

    @Value("${commons.api.account}")
    private String apiAccount;
    @Value("${commons.api.password}")
    private String apiPassword;

    private final CookieStore cookieStore = new BasicCookieStore();
    private String csrf;

    @PostConstruct
    void postConstruct() {
        csrf = getCsrfToken();
        LOGGER.info("Video2Commons CSRF token: {}", csrf);
    }

    private String getCsrfToken() {
        try (CloseableHttpClient httpclient = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy())
                .build()) {
            HttpClientContext context = getHttpClientContext();
            // STEP 1 - Request login page
            HttpRequestBase request = newHttpGet(URL_BASE + "oauthinit?returnto=" + URL_BASE);
            try (CloseableHttpResponse response = executeRequest(request, httpclient, context);
                    InputStream in = response.getEntity().getContent()) {
                request = newHttpPost(
                        Jsoup.parse(in, "UTF-8", CommonsService.BASE_URL).getElementsByTag("form").first(),
                        action -> CommonsService.BASE_URL + action,
                        (input, params) -> {
                            String name = input.attr("name");
                            params.add(new BasicNameValuePair(name, "wpName".equals(name) ? apiAccount
                                    : "wpPassword".equals(name) ? apiPassword : input.attr("value")));
                        }, null);
            }
            // STEP 2 - Login and get authorization request
            try (CloseableHttpResponse response = executeRequest(request, httpclient, context);
                    InputStream in = response.getEntity().getContent()) {
                request = newHttpPost(
                        Jsoup.parse(in, "UTF-8", CommonsService.BASE_URL).getElementById("mw-mwoauth-authorize-form"),
                        action -> CommonsService.BASE_URL + action,
                        (input, params) -> params.add(new BasicNameValuePair(input.attr("name"), input.attr("value"))),
                        (button, params) -> {
                            String name = button.attr("name");
                            if ("accept".equals(name)) {
                                params.add(new BasicNameValuePair(name, button.attr("value")));
                            }
                        });
            }
            // STEP 3 - Authorize application
            try (CloseableHttpResponse response = executeRequest(request, httpclient, context);
                    InputStream in = response.getEntity().getContent()) {
            }
            // STEP 4 - request CSRF token
            request = newHttpGet(URL_API + "/csrf");
            try (CloseableHttpResponse response = executeRequest(request, httpclient, context);
                    InputStream in = response.getEntity().getContent()) {
                return jackson.readValue(in, Csrf.class).csrf();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to initialize video2commons API. Upload of MP4 videos will not be possible: {}",
                    e.getMessage());
            return null;
        }
    }

    public Video2CommonsTask uploadVideo(String wikiCode, String filename, URL url)
            throws IOException {
        String filenameExt = requireNonNull(filename, "filename").replace(".mp4", "");
        HttpClientContext httpClientContext = getHttpClientContext();
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            // STEP 1 - Run task
            RunResponse run;
            HttpRequestBase request = Utils.newHttpPost(URL_API + "/task/run",
                    Map.of("url", url, "extractor", "", "subtitles", false, "filename", filenameExt, "filedesc",
                            wikiCode, "format", "webm (VP9/Opus)", "_csrf_token",
                            requireNonNull(csrf, "v2c csrf token")));
            try (CloseableHttpResponse response = executeRequest(request, httpclient, httpClientContext);
                    InputStream in = response.getEntity().getContent()) {
                run = jackson.readValue(in, RunResponse.class);
                if (run.error() != null) {
                    throw new IOException(run.toString());
                }
            }
            LOGGER.info("Started video2commons task {} to upload {} as '{}.webm'", run.id(), url, filenameExt);
            Video2CommonsTask task = repository.save(new Video2CommonsTask(run.id(), url, filenameExt + ".webm"));
            // STEP 2 - check status and wait a few seconds (just to check logs, tasks can
            // be pending several hours)
            request = Utils.newHttpGet(URL_API + "/status-single?task=" + run.id());
            int n = 1;
            int max = 10;
            while (!task.getStatus().isCompleted() && n++ < max) {
                try (CloseableHttpResponse response = executeRequest(request, httpclient, httpClientContext);
                        InputStream in = response.getEntity().getContent()) {
                    TaskStatusValue status = jackson.readValue(in, TaskStatus.class).value();
                    LOGGER.info("{} => {}", url, status);
                    task.setProgress(status.progress);
                    task.setStatus(status.status());
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOGGER.error(e.getMessage(), e);
                    Thread.currentThread().interrupt();
                } catch (IOException e) {
                    LOGGER.warn("{}", e.getMessage());
                }
                task.setLastChecked(ZonedDateTime.now());
                task = repository.save(task);
            }
            if (task.getProgress() < 100) {
                LOGGER.info("video2commons did not complete upload of {} yet. Last progress of task {}: {}%", url,
                        run.id(), task.getProgress());
            }
            return task;
        }
    }

    private CloseableHttpResponse executeRequest(HttpRequestBase request, CloseableHttpClient httpclient,
            HttpClientContext context) throws IOException {
        return checkResponse(request, httpclient.execute(request, context));
    }

    private HttpClientContext getHttpClientContext() {
        HttpClientContext context = new HttpClientContext();
        context.setCookieStore(cookieStore);
        context.setRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build());
        return context;
    }

    private static record Csrf(String csrf) {
    }

    private static record RunResponse(String error, String id, String step) {
    }

    private static record TaskStatus(TaskStatusValue value) {

    }

    private static record TaskStatusValue(String id, int progress, String status, String text, String title, URL url) {

    }
}
