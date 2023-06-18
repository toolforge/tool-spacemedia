package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import java.io.IOException;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;

@Service
public class AsyncOrgUpdaterService {

    @Async
    public void updateMedia(Org<?, ?, ?> org) throws IOException, UploadException {
        org.updateMedia();
    }
}
