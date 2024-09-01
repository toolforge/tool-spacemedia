package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.net.URL;

import org.apache.commons.lang3.StringUtils;

public record DvidsCredit(Integer id, String name, String rank, URL url) {

    public String dvidsCreditToString() {
        StringBuilder result = new StringBuilder();
        if (StringUtils.isNotBlank(rank())) {
            result.append(rank()).append(' ');
        }
        return result.append('[').append(url()).append(' ').append(name()).append(']').toString();
    }
}
