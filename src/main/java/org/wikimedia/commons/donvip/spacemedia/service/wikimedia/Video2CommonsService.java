package org.wikimedia.commons.donvip.spacemedia.service.wikimedia;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Objects.requireNonNull;
import static org.wikimedia.commons.donvip.spacemedia.data.domain.base.Video2CommonsTask.Status.DONE;
import static org.wikimedia.commons.donvip.spacemedia.data.domain.base.Video2CommonsTask.Status.PROGRESS;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.executeRequest;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.executeRequestStream;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.getHttpClientContext;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newHttpGet;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newHttpPost;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadataRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Video2CommonsTask;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Video2CommonsTask.Status;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Video2CommonsTaskRepository;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.AbstractOrgService;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Lazy
@Service
public class Video2CommonsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Video2CommonsService.class);

    private static final String URL_BASE = "https://video2commons.toolforge.org/";
    private static final String URL_API = URL_BASE + "api";

    public static final Set<String> V2C_VIDEO_EXTENSIONS = Set.of("avi", "mp4", "mov");

    @Autowired
    private ObjectMapper jackson;

    @Autowired
    private Video2CommonsTaskRepository repository;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private List<AbstractOrgService<?>> orgs;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private CommonsService commonsService;

    @Value("${commons.api.account}")
    private String apiAccount;
    @Value("${commons.api.password}")
    private String apiPassword;

    @Value("${video2commons.max.attempts:30}")
    private int maxAttempts;

    private final CookieStore cookieStore = new BasicCookieStore();
    private String csrf;

    @PostConstruct
    void postConstruct() {
        csrf = getCsrfToken();
        LOGGER.info("Video2Commons CSRF token: {}", csrf);
    }

    public boolean isPermittedFileExt(String ext) {
        return ext != null && V2C_VIDEO_EXTENSIONS.contains(ext);
    }

    private String getCsrfToken() {
        try (CloseableHttpClient httpclient = HttpClientBuilder.create().build()) {
            HttpClientContext context = getHttpClientContext(cookieStore);
            // STEP 1 - Request login page
            HttpUriRequestBase request = newHttpGet(URL_BASE + "oauthinit?returnto=" + URL_BASE);
            try (InputStream in = executeRequestStream(request, httpclient, context)) {
                request = newHttpPost(
                        Jsoup.parse(in, "UTF-8", URL_BASE).getElementsByTag("form").first(),
                        action -> CommonsService.BASE_URL + action,
                        (input, params) -> {
                            String name = input.attr("name");
                            params.add(new BasicNameValuePair(name, "wpName".equals(name) ? apiAccount
                                    : "wpPassword".equals(name) ? apiPassword : input.attr("value")));
                        }, null);
            }
            // STEP 2 - Login and get authorization request
            try (InputStream in = executeRequestStream(request, httpclient, context)) {
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
            // STEP 3 - Authorize application - follow POST redirects manually
            // HTTP Client 5 removed the LaxRedirectStrategy
            // https://stackoverflow.com/questions/69531010/how-to-follow-redirects-for-post-in-apache-httpclient-5-1
            try (CloseableHttpClient httpclient2 = HttpClientBuilder.create().disableRedirectHandling().build()) {
                try (ClassicHttpResponse response = executeRequest(request, httpclient2, context)) {
                    request = newHttpGet(response.getHeaders("Location")[0].getValue()
                            .replace("tools.wmflabs.org/video2commons", "video2commons.toolforge.org"));
                }
            }
            try (InputStream in = executeRequestStream(request, httpclient, context)) {
                // Response not needed
            }
            // STEP 4 - request CSRF token
            request = newHttpGet(URL_API + "/csrf");
            try (InputStream in = executeRequestStream(request, httpclient, context)) {
                return jackson.readValue(in, Csrf.class).csrf();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to initialize video2commons API. Upload of some videos will not be possible: {}",
                    e.getMessage());
            GlitchTip.capture(e);
            return null;
        }
    }

    private String csrfToken() {
        if (csrf == null) {
            csrf = getCsrfToken();
            LOGGER.info("Video2Commons CSRF token: {}", csrf);
        }
        return csrf;
    }

    public Video2CommonsTask uploadVideo(String wikiCode, String filename, URL url, String orgId,
            CompositeMediaId mediaId, Long metadataId, String format) throws IOException {
        Video2CommonsTask task = repository.findFirstByUrlOrMetadataIdAndStatusInOrderByCreatedDesc(url, metadataId,
                Set.of(PROGRESS, DONE));
        if (task != null) {
            LOGGER.warn("Upload requested but there is already an ongoing video2commons task, returning it: {}", task);
            return task;
        }
        String filenameExt = requireNonNull(filename, "filename");
        for (String ext : V2C_VIDEO_EXTENSIONS) {
            filenameExt = filenameExt.replace('.' + ext, "");
        }
        if (!wikiCode.contains("[[Category:Uploaded with video2commons]]")) {
            wikiCode += "\n[[Category:Uploaded with video2commons]]";
        }
        HttpClientContext httpClientContext = getHttpClientContext(cookieStore);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // STEP 1 - Run task
            RunResponse run = submitTaskRun(wikiCode, url, filenameExt, httpClientContext, httpClient, format);
            if (run.error() != null) {
                throw new IOException(run.toString());
            }
            task = repository
                    .save(new Video2CommonsTask(run.id(), url, filenameExt + ".webm", orgId, mediaId, metadataId));
            // STEP 2 - check status and wait a few seconds (just to check logs, tasks can
            // be pending several hours)
            HttpUriRequestBase request = newHttpGet(URL_API + "/status-single?task=" + run.id());
            int n = 1;
            while (!task.getStatus().isCompleted() && n++ < maxAttempts) {
                task = updateTask(task, request, url, httpClient, httpClientContext);
                // If there is no audio track, can't you just deal with it?!
                if ("webm (VP9/Opus)".equals(format) && task.isNoAudioTrackError()) {
                    LOGGER.info("No audio, fallback to webm (VP9) format for {}:{}:{}", orgId, mediaId, metadataId);
                    handleNoAudioTrackError(task, httpClientContext, httpClient);
                    return uploadVideo(wikiCode, filenameExt, url, orgId, mediaId, metadataId, "webm (VP9)");
                }
            }
            if (task.getProgress() < 100) {
                LOGGER.info("video2commons did not complete upload of {} yet. Last progress of task {}: {}%", url,
                        run.id(), task.getProgress());
            }
            return task;
        }
    }

    public void handleNoAudioTrackError(Video2CommonsTask task, HttpClientContext httpClientContext,
            CloseableHttpClient httpClient) {
        removeTask(task.getId(), httpClientContext, httpClient);
        repository.delete(task);
        FileMetadata fm = fileMetadataRepository.findById(task.getMetadataId())
                .orElseThrow(() -> new IllegalStateException("No file metadata found with id " + task.getMetadataId()));
        if (fm.isAudioTrack() != Boolean.FALSE) {
            fm.setAudioTrack(Boolean.FALSE);
            fileMetadataRepository.save(fm);
        }
    }

    private RunResponse submitTaskRun(String wikiCode, URL url, String filenameExt, HttpClientContext httpClientContext,
            CloseableHttpClient httpClient, String format) throws IOException {
        HttpUriRequestBase request = newHttpPost(URL_API + "/task/run",
                Map.of("url", url, "extractor", "", "subtitles", false, "filename", filenameExt, "filedesc",
                        wikiCode, "format", format, "_csrf_token", requireNonNull(csrfToken(), "v2c csrf token")));
        try (InputStream in = executeRequestStream(request, httpClient, httpClientContext)) {
            RunResponse run = jackson.readValue(in, RunResponse.class);
            LOGGER.info("video2commons task {} submitted to upload {} as '{}.webm' ({})", run.id(), url, filenameExt,
                    format);
            return run;
        }
    }

    public void abortTask(String taskId, HttpClientContext httpClientContext, CloseableHttpClient httpClient) {
        abortOrRemoveTask("abort", taskId, httpClientContext, httpClient);
    }

    public void removeTask(String taskId, HttpClientContext httpClientContext, CloseableHttpClient httpClient) {
        abortOrRemoveTask("remove", taskId, httpClientContext, httpClient);
    }

    private void abortOrRemoveTask(String op, String taskId, HttpClientContext httpClientContext,
            CloseableHttpClient httpClient) {
        try {
            String response = httpClient.execute(
                    newHttpPost(URL_API + "/task/" + op,
                            Map.of("id", taskId, "_csrf_token", requireNonNull(csrfToken(), "v2c csrf token"))),
                    httpClientContext, new BasicHttpClientResponseHandler());
            LOGGER.warn("{} video2commons task {} => {}", op, taskId, response);
        } catch (IOException e) {
            LOGGER.warn("Failed to {} video2commons task {}", op, taskId);
        }
    }

    private Video2CommonsTask updateTask(Video2CommonsTask task, HttpUriRequestBase request, URL url,
            CloseableHttpClient httpClient, HttpClientContext httpClientContext) {
        try (InputStream in = executeRequestStream(request, httpClient, httpClientContext)) {
            TaskStatusValue status = jackson.readValue(in, TaskStatus.class).value();
            if (status == null || Status.valueOf(status.status().toUpperCase(Locale.ENGLISH)).isFailed()) {
                LOGGER.error("{} => {}", url, status);
            } else {
                LOGGER.info("{} => {}", url, status);
            }
            if (status != null && !status.isUnknown()) {
                task.setProgress(status.progress);
                task.setStatus(status.status());
                task.setText(status.text());
                task.setLastChecked(ZonedDateTime.now());
            }
            Utils.sleep(1000);
        } catch (IOException e) {
            LOGGER.warn("{}", e.getMessage());
        }
        return repository.save(task);
    }

    public List<Video2CommonsTask> checkTasks(boolean forceDone) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            handleKnownFailedTasks(httpClient, getHttpClientContext(cookieStore));
        }
        if (forceDone) {
            addFilenamesOfSucceededTasks();
        }
        return updateTasks();
    }

    private List<Video2CommonsTask> updateTasks() throws IOException {
        List<Video2CommonsTask> result = new ArrayList<>();
        HttpClientContext httpClientContext = getHttpClientContext(cookieStore);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            for (Video2CommonsTask task : repository.findByStatusIn(Video2CommonsTask.Status.incompleteStates())) {
                result.add(updateTask(task, newHttpGet(URL_API + "/status-single?task=" + task.getId()),
                        task.getUrl(), httpClient, httpClientContext));
                if (task.getStatus() == DONE) {
                    LOGGER.info("Task done! => {}", task);
                    handleDoneTask(task);
                } else if (task.getStatus().isFailed()) {
                    LOGGER.error("Task failed! => {}", task);
                    handleFailedTask(task, httpClient, httpClientContext);
                } else if (task.getStatus() == PROGRESS && Duration.between(task.getCreated(), ZonedDateTime.now())
                        .toMillis() > Duration.of(23, HOURS).toMillis()) {
                    if (commonsService.findImage(task.getFilename().replace(' ', '_')) != null) {
                        LOGGER.info("Task done even if seen in progress! => {}", task);
                        handleDoneTask(task);
                    } else {
                        LOGGER.error("Task assumed failed after 23 hours! => {}", task);
                        handleFailedTask(task, httpClient, httpClientContext);
                    }
                    abortTask(task.getId(), httpClientContext, httpClient);
                    removeTask(task.getId(), httpClientContext, httpClient);
                    repository.delete(task);
                }
            }
        }
        return result;
    }

    private void handleDoneTask(Video2CommonsTask task) {
        orgs.stream().filter(o -> o.getId().equals(task.getOrgId())).findFirst()
                .ifPresent(o -> o.editStructuredDataContent(task.getFilename(), task.getMediaId(), task.getUrl()));
    }

    private void addFilenamesOfSucceededTasks() {
        for (Video2CommonsTask task : repository.findByStatusIn(Set.of(Video2CommonsTask.Status.DONE))) {
            orgs.stream().filter(o -> o.getId().equals(task.getOrgId())).findFirst()
                    .ifPresent(o -> {
                        FileMetadata metadata = o.retrieveMetadata(task.getMediaId(), task.getUrl());
                        if (metadata.getCommonsFileNames().isEmpty()) {
                            mediaService.saveNewMetadataCommonsFileNames(metadata, Set.of(task.getFilename()));
                        }
                    });
        }
    }

    private void handleKnownFailedTasks(CloseableHttpClient httpClient, HttpClientContext httpClientContext) {
        repository.findFailedTasks().forEach(x -> handleFailedTask(x, httpClient, httpClientContext));
    }

    private void handleFailedTask(Video2CommonsTask task, CloseableHttpClient httpClient,
            HttpClientContext httpClientContext) {
        orgs.stream().filter(o -> o.getId().equals(task.getOrgId())).findFirst().ifPresent(o -> {
            FileMetadata metadata = o.retrieveMetadata(task.getMediaId(), task.getUrl());
            Set<String> filenames = new TreeSet<>(metadata.getCommonsFileNames());
            for (String filename : metadata.getCommonsFileNames()) {
                if (commonsService.findImage(filename.replace(' ', '_')) == null) {
                    LOGGER.warn("V2C task {} => Removing '{}' file link from {}", task.getId(), filename, metadata);
                    filenames.remove(filename);
                }
            }
            if (!filenames.equals(metadata.getCommonsFileNames())) {
                mediaService.saveNewMetadataCommonsFileNames(metadata, filenames);
            }
        });
        if (task.isNoAudioTrackError()) {
            handleNoAudioTrackError(task, httpClientContext, httpClient);
        }
    }

    private static record Csrf(String csrf) {
    }

    private static record RunResponse(String error, String id, String step, String traceback) {
    }

    private static record TaskStatus(TaskStatusValue value) {
    }

    private static record TaskStatusValue(String id, int progress, String status, String text, String title, URL url) {
        boolean isUnknown() {
            return "The status of the task could not be retrieved.".equals(text);
        }
    }
}
