package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface WithKeywords {

    @JsonIgnore
    default Stream<String> getKeywordStream() {
        Set<String> kw = getKeywords();
        return kw != null ? kw.stream() : Stream.empty();
    }

    @JsonIgnore
    default boolean containsKeywords(Collection<String> keywords) {
        return getKeywordStream().anyMatch(keywords::contains);
    }

    Set<String> getKeywords();

    void setKeywords(Set<String> keywords);
}
