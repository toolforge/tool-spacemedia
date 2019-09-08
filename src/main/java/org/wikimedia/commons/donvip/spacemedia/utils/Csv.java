package org.wikimedia.commons.donvip.spacemedia.utils;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;

public final class Csv {

    private Csv() {
        // Hide default constructor
    }

    public static Map<String, String> loadMap(URL url) throws IOException {
        Map<String, String> result = new TreeMap<>();
        CsvMapper mapper = new CsvMapper();
        mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
        MappingIterator<String[]> it = mapper.readerFor(String[].class).readValues(url);
        it.next(); // Skip header
        while (it.hasNext()) {
            String[] row = it.next();
            result.put(row[0], row[1]);
        }
        return result;
    }
}
