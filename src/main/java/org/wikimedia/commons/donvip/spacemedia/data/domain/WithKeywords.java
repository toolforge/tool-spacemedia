package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.util.Set;

public interface WithKeywords {

    Set<String> getKeywords();

    void setKeywords(Set<String> keywords);
}
