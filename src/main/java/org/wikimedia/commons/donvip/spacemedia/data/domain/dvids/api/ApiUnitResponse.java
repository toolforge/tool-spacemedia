package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api;

import java.util.List;

public record ApiUnitResponse(List<String> messages, ApiPageInfo page_info, List<ApiUnitResult> results) {

}
