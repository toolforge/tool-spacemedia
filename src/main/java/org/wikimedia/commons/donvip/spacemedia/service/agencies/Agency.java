package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.temporal.Temporal;
import java.util.List;

import org.apache.lucene.misc.TermStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Problem;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.exception.TooManyResultsException;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;

public interface Agency<T extends Media<ID, D>, ID, D extends Temporal> {

    long countAllMedia();

    long countIgnored();

    long countMissingMedia();

    long countPerceptualHashes();

    long countUploadedMedia();

    Iterable<T> listAllMedia();

    Page<T> listAllMedia(Pageable page);

    List<T> listMissingMedia();

    Page<T> listMissingMedia(Pageable page);

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

    T uploadAndSave(String sha1) throws UploadException, TooManyResultsException;

    T upload(T media) throws UploadException;

    String getWikiHtmlPreview(String sha1) throws TooManyResultsException;

    String getWikiCode(String sha1) throws TooManyResultsException;

    String getWikiCode(T media, Metadata metadata);

    URL getSourceUrl(T media) throws MalformedURLException;

    List<T> searchMedia(String q);

    Page<T> searchMedia(String q, Pageable page);

    List<TermStats> getTopTerms() throws Exception;
}
