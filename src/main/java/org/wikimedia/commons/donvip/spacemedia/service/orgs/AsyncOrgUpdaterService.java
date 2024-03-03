package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import java.io.IOException;

import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;

@Lazy
@Service
public class AsyncOrgUpdaterService {

    @Async
    public void updateMedia(Org<?> org) throws IOException, UploadException {
        org.updateMedia(null);
    }
}
