package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.articles.NasaImageArticleMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.articles.NasaImageArticleMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;

@Service
public class NasaImageArticleService extends AbstractOrgService<NasaImageArticleMedia> {

    protected NasaImageArticleService(NasaImageArticleMediaRepository repository) {
        super(repository, "nasa.image.articles", Set.of());
    }

    @Override
    public String getName() {
        return "NASA (Image Articles)";
    }

    @Override
    public void updateMedia(String[] args) throws IOException, UploadException {
        // TODO Auto-generated method stub

    }

    @Override
    public URL getSourceUrl(NasaImageArticleMedia media, FileMetadata metadata) {
        return newURL("https://www.nasa.gov/image-article/" + media.getIdUsedInOrg());
    }

    @Override
    protected NasaImageArticleMedia refresh(NasaImageArticleMedia media) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Class<NasaImageArticleMedia> getMediaClass() {
        return NasaImageArticleMedia.class;
    }

}
