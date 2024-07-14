package org.wikimedia.commons.donvip.spacemedia.data.domain.dinamis.api;

import java.time.LocalDate;

public record DinamisProperties(int fid, String ds_name, LocalDate img_date, String f_quicklook, String mode_,
        double resolution, String page_) {
}
