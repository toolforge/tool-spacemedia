package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncSpaceAgencyUpdaterService {

    @Async
    public void updateMedia(SpaceAgency<?, ?, ?> agency) throws IOException {
        agency.updateMedia();
    }
}
