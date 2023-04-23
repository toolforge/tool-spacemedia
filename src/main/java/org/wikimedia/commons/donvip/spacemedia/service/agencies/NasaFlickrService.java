package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.net.MalformedURLException;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaMediaRepository;

@Service
public class NasaFlickrService extends AbstractAgencyFlickrService<NasaMedia, String, ZonedDateTime> {

    private static final List<String> STRINGS_TO_REMOVE = List.of(
            "<a href=\"http://instagram.com/NASAWebbTelescp\" rel=\"nofollow\">Follow us on Instagram</a>",
            "<a href=\"http://plus.google.com/+NASAWebbTelescope\" rel=\"nofollow\">Follow us on Google Plus</a>",
            "<a href=\"http://twitter.com/#!/NASAWebbTelescp\" rel=\"nofollow\">Follow us on Twitter</a>",
            "<a href=\"http://www.facebook.com/webbtelescope\" rel=\"nofollow\">Like us on Facebook</a>",
            "<a href=\"http://www.nasa.gov/audience/formedia/features/MP_Photo_Guidelines.html\" rel=\"nofollow\">NASA Image Use Policy</a>",
            "<a href=\"http://www.youtube.com/nasawebbtelescope\" rel=\"nofollow\">Subscribe to our YouTube channel</a>",
            "<b><a href=\"http://www.nasa.gov/audience/formedia/features/MP_Photo_Guidelines.html\">NASA image use policy.</a></b>",
            "<b><a href=\"http://www.nasa.gov/audience/formedia/features/MP_Photo_Guidelines.html\" rel=\"nofollow\">NASA image use policy.</a></b>",
            "<b><a href=\"http://www.nasa.gov/centers/goddard/home/index.html\" rel=\"nofollow\">NASA Goddard Space Flight Center</a></b> enables NASA’s mission through four scientific endeavors: Earth Science, Heliophysics, Solar System Exploration, and Astrophysics. Goddard plays a leading role in NASA’s accomplishments by contributing compelling scientific knowledge to advance the Agency’s mission.",
            "<b><a href=\"http://www.nasa.gov/centers/goddard/home/index.html\">NASA Goddard Space Flight Center</a></b> enables NASA’s mission through four scientific endeavors: Earth Science, Heliophysics, Solar System Exploration, and Astrophysics. Goddard plays a leading role in NASA’s accomplishments by contributing compelling scientific knowledge to advance the Agency’s mission.",
            "<b>Find us on <a href=\"http://instagrid.me/nasagoddard/?vm=grid\" rel=\"nofollow\">Instagram</a></b>",
            "<b>Find us on <a href=\"http://www.instagram.com/nasagoddard/?vm=grid\">Instagram</a></b>",
            "<b>Find us on <a href=\"https://twitter.com/NASAHubble\">Twitter</a>, <a href=\"https://instagram.com/nasahubble?utm_medium=copy_link\">Instagram</a>, <a href=\"https://www.facebook.com/NASAHubble\">Facebook</a> and <a href=\"https://www.youtube.com/playlist?list=PL3E861DC9F9A8F2E9\"> YouTube</a></b>",
            "<b>Follow us on <a href=\"http://twitter.com/NASA_GoddardPix\" rel=\"nofollow\">Twitter</a></b>",
            "<b>Follow us on <a href=\"http://twitter.com/NASAGoddardPix\">Twitter</a></b>",
            "<b>Like us on <a href=\"http://www.facebook.com/pages/Greenbelt-MD/NASA-Goddard/395013845897?ref=tsd\">Facebook</a></b>",
            "<b>Like us on <a href=\"http://www.facebook.com/pages/Greenbelt-MD/NASA-Goddard/395013845897?ref=tsd\" rel=\"nofollow\">Facebook</a></b>"
            );

    @Autowired
    private NasaMediaRepository<NasaMedia> nasaMediaRepository;

    @Autowired
    public NasaFlickrService(FlickrMediaRepository repository,
            @Value("${nasa.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, "nasa.flickr", flickrAccounts);
    }

    @Override
    public void updateMedia() {
        updateFlickrMedia();
    }

    @Override
    public String getName() {
        return "NASA (Flickr)";
    }

    @Override
    protected NasaMediaRepository<NasaMedia> getOriginalRepository() {
        return nasaMediaRepository;
    }

    @Override
    protected String getOriginalId(String id) {
        return id;
    }

    @Override
    protected Collection<String> getStringsToRemove(String pathAlias) {
        return STRINGS_TO_REMOVE;
    }

    @Override
    protected String getSource(FlickrMedia media) throws MalformedURLException {
        return super.getSource(media) + "\n{{NASA-image|id=" + media.getId() + "|center=}}";
    }

    @Override
    public Set<String> findTemplates(FlickrMedia media) {
        Set<String> result = super.findTemplates(media);
        result.add("PD-USGov-NASA");
        return result;
    }

    @Override
    protected Set<String> getTwitterAccounts(FlickrMedia uploadedMedia) {
        return Set.of("@NASA");
    }
}
