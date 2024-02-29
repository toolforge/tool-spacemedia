package org.wikimedia.commons.donvip.spacemedia.service.nasa;

import java.io.IOException;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper;

@Lazy
@Service
public class NasaMappingService {

    private Map<String, Map<String, String>> nasaInstruments;
    private Map<String, Map<String, String>> nasaMissions;
    private Map<String, String> nasaKeywords;

    @PostConstruct
    void init() throws IOException {
        nasaInstruments = CsvHelper.loadCsvMapMapping("nasa.instruments.csv");
        nasaMissions = CsvHelper.loadCsvMapMapping("nasa.missions.csv");
        nasaKeywords = CsvHelper.loadCsvMapping("nasa.keywords.csv");
    }

    public Map<String, Map<String, String>> getNasaInstruments() {
        return nasaInstruments;
    }

    public Map<String, Map<String, String>> getNasaMissions() {
        return nasaMissions;
    }

    public Map<String, String> getNasaKeywords() {
        return nasaKeywords;
    }
}
