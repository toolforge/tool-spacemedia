package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.tuple.Triple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Problem;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.TooManyResultsException;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;

public interface Agency<T extends Media<ID, D>, ID, D extends Temporal> {

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
     * Returns the space agency name, used in statistics and logs.
     *
     * @return the space agency name
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

    T uploadAndSaveBySha1(String sha1) throws UploadException, TooManyResultsException;

    T uploadAndSaveById(String id) throws UploadException, TooManyResultsException;

    Triple<T, Collection<Metadata>, Integer> upload(T media, boolean checkUnicity) throws UploadException;

    T refreshAndSaveById(String id) throws ImageNotFoundException, IOException;

    T refreshAndSave(T media) throws IOException;

    String getWikiHtmlPreview(String sha1) throws TooManyResultsException;

    String getWikiCode(String sha1) throws TooManyResultsException;

    String getWikiCode(T media, Metadata metadata);

    URL getSourceUrl(T media) throws MalformedURLException;

    List<T> searchMedia(String q);

    Page<T> searchMedia(String q, Pageable page);

    T saveMedia(T media);
}
