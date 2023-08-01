package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import java.io.IOException;
import java.net.URL;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Problem;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.TooManyResultsException;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;

public interface Org<T extends Media<ID, D>, ID, D extends Temporal> {

    void evictCaches();

    long countAllMedia();

    long countIgnored();

    long countMissingMedia();

    long countMissingImages();

    long countMissingVideos();

    long countPerceptualHashes();

    long countUploadedMedia();

    Iterable<T> listAllMedia();

    Page<T> listAllMedia(Pageable page);

    List<T> listMissingMedia();

    Page<T> listMissingMedia(Pageable page);

    Page<T> listMissingImages(Pageable page);

    Page<T> listMissingVideos(Pageable page);

    Page<T> listHashedMedia(Pageable page);

    List<T> listUploadedMedia();

    Page<T> listUploadedMedia(Pageable page);

    List<T> listDuplicateMedia();

    List<T> listIgnoredMedia();

    Page<T> listIgnoredMedia(Pageable page);

    /**
     * Returns the space org name, used in statistics and logs.
     *
     * @return the space org name
     */
    String getName();

    /**
     * Returns an unique identifier used for REST controllers.
     *
     * @return an unique identifier based on class name
     */
    String getId();

    void updateMedia() throws IOException, UploadException;

    Statistics getStatistics(boolean details);

    List<Problem> getProblems();

    Page<Problem> getProblems(Pageable page);

    long getProblemsCount();

    T getById(String id) throws ImageNotFoundException;

    void deleteById(String id) throws ImageNotFoundException;

    T uploadAndSaveBySha1(String sha1, boolean isManual) throws UploadException, TooManyResultsException;

    T uploadAndSaveById(String id, boolean isManual) throws UploadException, TooManyResultsException;

    Triple<T, Collection<FileMetadata>, Integer> upload(T media, boolean checkUnicity, boolean isManual)
            throws UploadException;

    T refreshAndSaveById(String id) throws ImageNotFoundException, IOException;

    T refreshAndSave(T media) throws IOException;

    String getWikiHtmlPreview(String sha1) throws TooManyResultsException;

    String getWikiCode(String sha1) throws TooManyResultsException;

    Pair<String, Map<String, String>> getWikiCode(T media, FileMetadata metadata);

    default URL getSourceUrl(T media) {
        // Used in web app
        return getSourceUrl(media, media.getMetadata().iterator().next());
    }

    URL getSourceUrl(T media, FileMetadata metadata);

    List<T> searchMedia(String q);

    Page<T> searchMedia(String q, Pageable page);

    T saveMedia(T media);
}
