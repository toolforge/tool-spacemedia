package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncAgencyUpdaterService {

    @Async
    public void updateMedia(Agency<?, ?, ?> agency) throws IOException {
        agency.updateMedia();
    }
}
