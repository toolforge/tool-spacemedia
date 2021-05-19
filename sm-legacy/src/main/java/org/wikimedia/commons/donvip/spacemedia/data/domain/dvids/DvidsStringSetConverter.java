package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.util.StdConverter;

public class DvidsStringSetConverter extends StdConverter<String, Set<String>> {

    @Override
    public Set<String> convert(String value) {
        return value != null ? Arrays.stream(value.split(",")).map(String::trim).collect(Collectors.toSet()) : null;
    }
}
