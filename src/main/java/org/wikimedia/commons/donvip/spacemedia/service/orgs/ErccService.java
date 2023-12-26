package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Locale.ENGLISH;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.getWithJsoup;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newHttpGet;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.EchoMapType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.ErccMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.ErccMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.api.Continent;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.api.Country;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.api.EventType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.api.GetPagedMapsResponse;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.api.MapsItem;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ErccService extends AbstractOrgService<ErccMedia> {

    private static final String ERCC_BASE_URL = "https://erccportal.jrc.ec.europa.eu/";

    private static final String ERCC_MAPS_PATH = "ECHO-Products/Maps#/maps/";

    private static final Logger LOGGER = LoggerFactory.getLogger(ErccService.class);

    @Autowired
    private ObjectMapper jackson;

    public ErccService(ErccMediaRepository repository) {
        super(repository, "ercc", Arrays.stream(EchoMapType.values()).map(EchoMapType::name).collect(toSet()));
    }

    @Override
    public String getName() {
        return "ERCC";
    }

    @Override
    public Set<String> findLicenceTemplates(ErccMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("PD-European-Commission");
        result.add("Cc-by-4.0"); // https://commission.europa.eu/legal-notice_en
        return result;
    }

    @Override
    public Set<String> findCategories(ErccMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        int year = media.getYear().getValue();
        boolean yearMapCatAdded = addLocationMapCat(result, year, media.getMainCountry());
        for (String country : media.getCountries()) {
            yearMapCatAdded |= addLocationMapCat(result, year, country);
        }
        if (isBlank(media.getMainCountry()) || media.getCountries().size() >= 3) {
            yearMapCatAdded |= addLocationMapCat(result, year, media.getContinent());
        }
        if (!yearMapCatAdded) {
            result.add(year + " maps");
        }
        String cat = "ECHO " + media.getMapType() + " Maps";
        if (EchoMapType.Base == media.getMapType()) {
            result.add(cat);
        } else {
            result.add(cat + " of " + year);
            media.getEventTypes().forEach(event -> ofNullable(switch (event) {
            case "Cold Wave" -> "cold waves";
            case "Assault", "Conflict" -> "conflicts";
            case "Drought" -> "droughts";
            case "Earthquake" -> "earthquakes";
            case "Epidemic" -> "diseases";
            case "Flash Flood", "Flood" -> "floods";
            case "Food Security", "Nutrition" -> "food security";
            case "Heat Wave" -> "heatwaves";
            case "Landslide", "Mud Slide" -> "landslides";
            case "Meteo Warning" -> "weather warnings";
            case "Population Displacement" -> "displaced persons";
            case "Extratropical Cyclone", "Storm Surge", "Tornado", "Tropical Cyclone" -> "storms";
            case "Tsunami" -> "tsunamis";
            case "Volcanic eruption" -> "volcanic eruptions";
            case "Fire", "Forest Fire", "Wild fire" -> "wildfires";
            case "Complex Emergency", "Factsheet", "Health", "Insect Infestation", "Resources", "Severe Weather",
                    "Snow Avalanche", "Technological Disaster", "UCPM", "Unrest" ->
                null;
            default -> null;
            }).ifPresent(x -> result.add(cat + " of " + x)));
        }
        return result;
    }

    boolean addLocationMapCat(Set<String> result, int year, String location) {
        if (isNotBlank(location)) {
            String cat = year + " maps of " + location;
            result.add(commonsService.existsCategoryPage(cat) ? cat : "Maps of " + location);
        }
        return false;
    }

    @Override
    protected SdcStatements getStatements(ErccMedia media, FileMetadata metadata) {
        SdcStatements result = super.getStatements(media, metadata).instanceOf(WikidataItem.Q4006_MAP)
                .creator("Q818564"); // Created by ERCC
        ofNullable(media.getMainCountry()).ifPresent(mainCountry -> wikidata.searchCountry(mainCountry)
                .or(() -> wikidata.searchContinent(mainCountry)).map(Pair::getKey).ifPresent(result::depicts));
        return result;
    }

    @Override
    public URL getSourceUrl(ErccMedia media, FileMetadata metadata) {
        return newURL(ERCC_BASE_URL + ERCC_MAPS_PATH + media.getIdUsedInOrg());
    }

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    protected String getAuthor(ErccMedia media) {
        return "ERCC - Emergency Response Coordination Centre"
                + (isBlank(media.getSources()) ? "" : "\n\nSources: " + media.getSources());
    }

    @Override
    public void updateMedia(String[] args) throws IOException, UploadException {
        LocalDateTime start = startUpdateMedia();
        int count = 0;
        boolean loop = true;
        int idx = 1;
        List<FileMetadata> uploadedMetadata = new ArrayList<>();
        List<ErccMedia> uploadedMedia = new ArrayList<>();
        LocalDate doNotFetchEarlierThan = getRuntimeData().getDoNotFetchEarlierThan();
        while (loop) {
            String url = ERCC_BASE_URL + "API/ERCC/Maps/GetPagedItems?ItemsCurrentPageIndex=" + idx;
            if (doNotFetchEarlierThan != null) {
                url += "&Filter%5BDateFrom%5D=" + doNotFetchEarlierThan;
            }
            try {
                GetPagedMapsResponse response = jackson.readValue(newURL(url), GetPagedMapsResponse.class);
                if (idx == 1) {
                    LOGGER.info("Found {} items to process", response.TotalCount());
                }
                if (response.TotalCount() > 0) {
                    for (MapsItem item : response.Items()) {
                        try {
                            updateMapItem(item, uploadedMedia, uploadedMetadata);
                        } catch (RuntimeException e) {
                            LOGGER.error("Error when updating {}", item, e);
                        }
                        ongoingUpdateMedia(start, count++);
                    }
                }
                loop = ++idx <= response.NumberOfPages();
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Error when fetching {}", url, e);
            }
        }
        endUpdateMedia(count, uploadedMedia, uploadedMetadata, start);
    }

    protected ErccMedia updateMapItem(MapsItem item, List<ErccMedia> uploadedMedia,
            List<FileMetadata> uploadedMetadata) throws IOException, UploadException {
        ErccMedia media;
        boolean save = false;
        CompositeMediaId id = new CompositeMediaId(item.getEchoMapType().name(),
                Integer.toString(item.ContentItemId()));
        Optional<ErccMedia> mediaInRepo = repository.findById(id);
        if (mediaInRepo.isPresent()) {
            media = mediaInRepo.get();
        } else {
            media = mapMedia(item, id);
            save = true;
        }
        if (doCommonUpdate(media)) {
            save = true;
        }
        if (shouldUploadAuto(media, false)) {
            Triple<ErccMedia, Collection<FileMetadata>, Integer> upload = upload(save ? saveMedia(media) : media, true,
                    false);
            uploadedMedia.add(saveMedia(upload.getLeft()));
            uploadedMetadata.addAll(upload.getMiddle());
            save = false;
        }
        return saveMediaOrCheckRemote(save, media);
    }

    protected ErccMedia mapMedia(MapsItem item, CompositeMediaId id) {
        ErccMedia media = new ErccMedia();
        media.setId(id);
        media.setMapType(item.getEchoMapType());
        media.setSources(item.SourceList());
        if (isNotEmpty(item.EventTypes())) {
            media.setEventTypes(item.EventTypes().stream().map(EventType::Name).collect(toSet()));
        }
        ofNullable(item.Continent()).map(Continent::Name).ifPresent(media::setContinent);
        ofNullable(item.Country()).map(Country::Name).ifPresent(media::setMainCountry);
        media.setCountries(item.Countries().stream().map(Country::Name).collect(toSet()));
        media.setCategory(item.CategoryCode());
        media.setDescription(item.Description());
        media.setTitle(item.Title());
        media.setPublicationDate(item.PublishedOnDate().toLocalDate());
        if (item.CreatedOnDate().getYear() >= 2000) {
            media.setCreationDate(item.CreatedOnDate().toLocalDate());
        }
        ofNullable(item.MainFileExtension()).ifPresent(x -> addMetadata(media, item.MainDownloadPath(),
                fm -> {
                    fm.setExtension(x.toLowerCase(ENGLISH));
                    fm.setOriginalFileName(item.MainFileName());
                }));
        ofNullable(item.SecondFileExtension()).ifPresent(x -> addMetadata(media, item.SecondDownloadPath(),
                fm -> {
                    fm.setExtension(x.toLowerCase(ENGLISH));
                    fm.setOriginalFileName(item.SecondFileName());
                }));
        if (!media.hasMetadata()) {
            ofNullable(item.ImageExtension()).ifPresent(
                    x -> addMetadata(media, item.ImageDownloadPath(), fm -> fm.setExtension(x.toLowerCase(ENGLISH))));
        }
        ofNullable(item.ThumbnailExtension()).ifPresent(x -> media.setThumbnailUrl(item.ThumbnailWebPath()));
        return media;
    }

    @Override
    protected ErccMedia refresh(ErccMedia media) throws IOException {
        HttpGet request = newHttpGet(
                newURL(ERCC_BASE_URL + "API/ERCC/Maps/GetMap?mapID=" + media.getId().getMediaId()));
        request.setHeader("RequestVerificationToken",
                getWithJsoup(ERCC_BASE_URL + ERCC_MAPS_PATH + media.getIdUsedInOrg(), 15_000, 3)
                        .getElementsByAttributeValue("name", "__RequestVerificationToken").first().attr("value"));
        try (CloseableHttpClient httpclient = HttpClients.createDefault();
                CloseableHttpResponse response = httpclient.execute(request);
                InputStream in = response.getEntity().getContent()) {
            if (response.getStatusLine().getStatusCode() >= 400) {
                LOGGER.error("{} => {}", request, response.getStatusLine());
                return media;
            }
            return media.copyDataFrom(mapMedia(jackson.readValue(in, MapsItem.class), media.getId()));
        }
    }

    @Override
    protected Class<ErccMedia> getMediaClass() {
        return ErccMedia.class;
    }

    @Override
    protected Set<String> getEmojis(ErccMedia uploadedMedia) {
        Set<String> result = super.getEmojis(uploadedMedia);
        result.add(Emojis.MAP);
        return result;
    }

    @Override
    protected Set<String> getMastodonAccounts(ErccMedia uploadedMedia) {
        return Set.of("@EC_ECHO@social.network.europa.eu");
    }

    @Override
    protected Set<String> getTwitterAccounts(ErccMedia uploadedMedia) {
        return Set.of("@eu_echo");
    }
}
