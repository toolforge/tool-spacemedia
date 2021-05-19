package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.util.Set;

import com.fasterxml.jackson.databind.util.StdConverter;

public class DvidsSetStringConverter extends StdConverter<Set<String>, String> {

    @Override
    public String convert(Set<String> value) {
        return value != null ? String.join(", ", value) : null;
    }
}
