package org.wikimedia.commons.donvip.spacemedia.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;

public final class CsvHelper {

    private CsvHelper() {
        // Hide default constructor
    }

    public static Set<String> loadSet(URL url) throws IOException {
        CsvMapper mapper = new CsvMapper();
        mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
        return mapper.readerFor(String[].class).readValues(url).readAll().stream().map(a -> ((String[]) a)[0])
                .collect(Collectors.toSet());
    }

    public static Map<String, String> loadMap(URL url) throws IOException {
        if (url == null) {
            return null;
        }
        Map<String, String> result = new TreeMap<>();
        CsvMapper mapper = new CsvMapper();
        mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
        try (InputStreamReader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
            MappingIterator<String[]> it = mapper.readerFor(String[].class).readValues(reader);
            it.next(); // Skip header
            while (it.hasNext()) {
                String[] row = it.next();
                result.put(row[0], row[1]);
            }
        }
        return result;
    }
}
