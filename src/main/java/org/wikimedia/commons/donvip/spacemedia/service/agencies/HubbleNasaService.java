package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble.HubbleNasaImageFiles;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble.HubbleNasaImageResponse;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble.HubbleNasaImagesResponse;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble.HubbleNasaMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble.HubbleNasaMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble.HubbleNasaNewsReleaseResponse;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble.HubbleNasaNewsResponse;

/**
 * Service harvesting images from NASA Hubble / Jame Webb websites.
 */
@Service
public class HubbleNasaService extends AbstractFullResSpaceAgencyService<HubbleNasaMedia, Integer, ZonedDateTime> {

	private static final DateTimeFormatter dateformatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);

	private static final Logger LOGGER = LoggerFactory.getLogger(HubbleNasaService.class);

	@Value("${hubble.nasa.news.link}")
	private String newsEndpoint;

	@Value("${hubble.nasa.news.detail.link}")
	private String newsDetailEndpoint;

	@Value("${hubble.nasa.search.link}")
	private String searchEndpoint;

	@Value("${hubble.nasa.detail.link}")
	private String detailEndpoint;

	@Value("${hubble.nasa.hubble.image.link}")
	private String hubbleImageLink;

	@Value("${hubble.nasa.james_webb.image.link}")
	private String webbImageLink;

    @Autowired
    public HubbleNasaService(HubbleNasaMediaRepository repository) {
        super(repository);
    }

    @Override
    protected Class<HubbleNasaMedia> getMediaClass() {
        return HubbleNasaMedia.class;
    }

	private HubbleNasaImageResponse getImageDetails(RestTemplate rest, int imageId) {
		return rest.getForObject(detailEndpoint.replace("<id>", Integer.toString(imageId)),
				HubbleNasaImageResponse.class);
	}

    @Override
    @Scheduled(fixedRateString = "${hubble.nasa.update.rate}", initialDelayString = "${initial.delay}")
    public void updateMedia() throws IOException {
		LocalDateTime start = startUpdateMedia();
		RestTemplate rest = new RestTemplate();
		int count = 0;
		// Step 1: loop over news, as they include more details than what is returned by images API
		boolean loop = true;
		int idx = 1;
		while (loop) {
			String urlLink = newsEndpoint.replace("<idx>", Integer.toString(idx++));
			HubbleNasaNewsResponse[] response = rest.getForObject(urlLink, HubbleNasaNewsResponse[].class);
			loop = response.length > 0;
			for (HubbleNasaNewsResponse news : response) {
				HubbleNasaNewsReleaseResponse details = rest.getForObject(newsDetailEndpoint.replace("<id>",
						news.getId()), HubbleNasaNewsReleaseResponse.class);
				for (int imageId : details.getReleaseImages()) {
					try {
						count += doUpdateMedia(imageId, getImageDetails(rest, imageId), details);
					} catch (IOException | URISyntaxException | RuntimeException e) {
						LOGGER.error("Error while fetching Hubble news: " + news, e);
					}
				}
			}
		}
		// Step 2: loop over images to find ones not news-related
		loop = true;
		idx = 1;
		while (loop) {
			String urlLink = searchEndpoint.replace("<idx>", Integer.toString(idx++));
			HubbleNasaImagesResponse[] response = rest.getForObject(urlLink, HubbleNasaImagesResponse[].class);
			loop = response.length > 0;
			for (HubbleNasaImagesResponse image : response) {
				try {
					count += doUpdateMedia(image.getId(), getImageDetails(rest, image.getId()), null);
				} catch (IOException | URISyntaxException | RuntimeException e) {
					LOGGER.error("Error while fetching Hubble images: " + image, e);
				}
			}
		}
		endUpdateMedia(count, start);
    }

	private static URL toUrl(String fileUrl) throws MalformedURLException {
		if (fileUrl.startsWith("//")) {
			fileUrl = "https:" + fileUrl;
		}
		if (fileUrl.startsWith("https://imgsrc.hubblesite.org/")) {
			// Broken https, redirected anyway to https://hubblesite.org/ without hvi folder
			fileUrl = fileUrl.replace("imgsrc.", "").replace("/hvi/", "/");
		}
		return new URL(fileUrl);
	}

	private static boolean endsWith(URL url, String... exts) {
		if (url != null) {
			String externalForm = url.toExternalForm();
			for (String ext : exts) {
				if (externalForm.endsWith(ext)) {
					return true;
				}
			}
		}
		return false;
	}

	private int doUpdateMedia(int id, HubbleNasaImageResponse image, HubbleNasaNewsReleaseResponse news)
			throws IOException, URISyntaxException {
        boolean save = false;
		HubbleNasaMedia media;
		Optional<HubbleNasaMedia> mediaInRepo = repository.findById(id);
        if (mediaInRepo.isPresent()) {
            media = mediaInRepo.get();
        } else {
			media = new HubbleNasaMedia();
            media.setId(id);
			media.setDescription(image.getDescription());
			media.setTitle(image.getName());
			media.setCredits(image.getCredits());
			media.setMission(image.getMission());
			for (HubbleNasaImageFiles imageFile : image.getImageFiles()) {
				String fileUrl = imageFile.getFileUrl();
				URL url = toUrl(fileUrl);
				if (fileUrl.endsWith(".tif") || fileUrl.endsWith(".tiff")) {
					media.setFullResAssetUrl(url);
				} else if (fileUrl.endsWith(".png") || fileUrl.endsWith(".jpg")) {
					media.setAssetUrl(url);
				}
			}
			if (media.getFullResAssetUrl() == null && image.getImageFiles().size() > 1) {
				image.getImageFiles().stream().max(Comparator.comparingInt(HubbleNasaImageFiles::getFileSize))
						.map(HubbleNasaImageFiles::getFileUrl).ifPresent(max -> {
							try {
								media.setFullResAssetUrl(toUrl(max));
							} catch (MalformedURLException e) {
								LOGGER.error(max, e);
							}
						});
			}
			if (endsWith(media.getFullResAssetUrl(), ".png", ".jpg") && endsWith(media.getAssetUrl(), ".png", ".jpg")) {
				media.setAssetUrl(media.getFullResAssetUrl());
				media.setFullResAssetUrl(null);
			}
			save = true;
        }
		if (media.getDate() == null && news != null) {
			media.setDate(news.getPublication());
			save = true;
		}
		if (media.getNewsId() == null && news != null) {
			media.setNewsId(news.getId());
			save = true;
		}
		if (CollectionUtils.isEmpty(media.getKeywords())) {
			URL sourceUrl = getSourceUrl(media);
			if (sourceUrl != null) {
				String sourceLink = sourceUrl.toExternalForm();
				LOGGER.info(sourceLink);
				Document html = Jsoup.connect(sourceLink).timeout(60_000).get();
				media.setKeywords(
					html.getElementsByClass("keyword-tag").stream().map(Element::text).collect(Collectors.toSet()));
				Elements tds = html.getElementsByTag("td");
				if (StringUtils.isEmpty(media.getObjectName())) {
					findTd(tds, "Object Name").ifPresent(media::setObjectName);
				}
				if (media.getExposureDate() == null) {
					findTd(tds, "Exposure Dates").ifPresent(dates -> {
						try {
							media.setExposureDate(LocalDate.parse(dates, dateformatter));
						} catch (DateTimeParseException e) {
							LOGGER.debug(dates, e);
						}
					});
				}
				save = true;
			}
		}
		if (mediaService.updateMedia(media)) {
			save = true;
		}
		if (save) {
			repository.save(media);
		}
		return save ? 1 : 0;
	}

	private static Optional<String> findTd(Elements tds, String label) {
		List<Element> matches = tds.stream().filter(x -> label.equalsIgnoreCase(x.text())).collect(Collectors.toList());
		if (matches.size() == 2) {
			return Optional.ofNullable(matches.get(0).nextElementSibling()).map(Element::text);
		}
		return Optional.empty();
	}

	@Override
    public String getName() {
        return "Hubble (NASA)";
    }

    @Override
    public List<String> findTemplates(HubbleNasaMedia media) {
        List<String> result = super.findTemplates(media);
        result.add("PD-Hubble");
        return result;
    }

    @Override
	protected Set<String> findCategories(HubbleNasaMedia media) {
		Set<String> result = super.findCategories(media);
		for (String keyword : media.getKeywords()) {
			switch (keyword) {
			// TODO
			}
		}
		return result;
	}

	@Override
    public URL getSourceUrl(HubbleNasaMedia media) throws MalformedURLException {
		if (media.getNewsId() != null && media.getMission() != null) {
			String pattern = getimageLinkPattern(media.getMission());
			if (pattern != null) {
				return new URL(pattern.replace("<news_id>", media.getNewsId().replace('-', '/')).replace("<img_id>",
						media.getId().toString()));
			}
		}
		return null;
    }

	private String getimageLinkPattern(String mission) {
		switch (mission) {
		case "hubble":
			return hubbleImageLink;
		case "james_webb":
			return webbImageLink;
		default:
			return null;
		}
	}

    @Override
    protected String getAuthor(HubbleNasaMedia media) throws MalformedURLException {
		return media.getCredits();
    }

	@Override
	protected Optional<Temporal> getCreationDate(HubbleNasaMedia media) {
		return Optional.ofNullable(media.getExposureDate());
	}

	@Override
	protected Optional<Temporal> getUploadDate(HubbleNasaMedia media) {
		return Optional.of(media.getDate());
	}
}
