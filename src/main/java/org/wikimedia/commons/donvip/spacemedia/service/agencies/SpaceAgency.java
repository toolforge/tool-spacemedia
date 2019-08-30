package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.temporal.Temporal;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Problem;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.xml.sax.SAXException;

public interface SpaceAgency<T extends Media<ID>, ID> {

    void checkCommonCategories();

    long countAllMedia();

    long countIgnored();

    long countMissingMedia();

    long countUploadedMedia();

    Iterable<T> listAllMedia();

    Page<T> listAllMedia(Pageable page);

    List<T> listMissingMedia();

    Page<T> listMissingMedia(Pageable page);

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

    void updateMedia() throws IOException;

    Statistics getStatistics();

    List<Problem> getProblems();

    long getProblemsCount();

    T upload(String sha1) throws IOException;

    String getWikiHtmlPreview(String sha1)
            throws ClientProtocolException, IOException, ParserConfigurationException, SAXException;

    String getWikiCode(String sha1);

    String getWikiCode(T media);

    URL getSourceUrl(T media) throws MalformedURLException;

    URL getThumbnailUrl(T media);

    Temporal getDate(T media);
}
