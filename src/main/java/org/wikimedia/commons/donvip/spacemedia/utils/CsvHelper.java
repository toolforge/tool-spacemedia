package org.wikimedia.commons.donvip.spacemedia.utils;

import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;

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
                .collect(toSet());
    }

    public static Map<String, String> loadMap(URL url) throws IOException {
        return doLoadMap(url, (header, row) -> row[1]);
    }

    public static Map<String, Map<String, String>> loadMapMap(URL url) throws IOException {
        return doLoadMap(url, (header, row) -> {
            Map<String, String> map = new TreeMap<>();
            for (int i = 1; i < header.length; i++) {
                if (i < row.length) {
                    map.put(header[i], row[i]);
                }
            }
            return map;
        });
    }

    private static <V> Map<String, V> doLoadMap(URL url, BiFunction<String[], String[], V> valueMapper)
            throws IOException {
        if (url == null) {
            return Map.of();
        }
        Map<String, V> result = new TreeMap<>();
        CsvMapper mapper = new CsvMapper();
        mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
        try (InputStreamReader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
            MappingIterator<String[]> it = mapper.readerFor(String[].class).readValues(reader);
            String[] header = it.next();
            while (it.hasNext()) {
                String[] row = it.next();
                result.put(row[0], valueMapper.apply(header, row));
            }
        }
        return result;
    }
}
