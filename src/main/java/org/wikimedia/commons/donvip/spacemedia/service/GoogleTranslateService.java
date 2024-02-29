package org.wikimedia.commons.donvip.spacemedia.service;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import javax.annotation.PostConstruct;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.Translation;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.cloud.translate.v3.TranslationServiceSettings;

@Lazy
@Service
public class GoogleTranslateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleTranslateService.class);

    @Value("${google.translate.project}")
    private String project;

    @Value("${google.translate.location}")
    private String location;

    @Value("${google.translate.privateKeyId}")
    private String privateKeyId;

    @Value("${google.translate.privateKey}")
    private String privateKey;

    @Value("${google.translate.clientEmail}")
    private String clientEmail;

    @Value("${google.translate.clientId}")
    private String clientId;

    @PostConstruct
    public void postConstruct() {
        privateKey = privateKey.replace("\\\\n", "\n");
        try {
            withTranslationClient(translationServiceClient -> {
                LOGGER.info("Google Translation configuration OK.");
                return "";
            });
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Cannot create Google Translation client: {}", e.getMessage());
            LOGGER.warn("google.translate.project: {}", project);
            LOGGER.warn("google.translate.location: {}", location);
            LOGGER.warn("google.translate.privateKeyId: {}", privateKeyId);
            LOGGER.warn("google.translate.privateKey: {}", privateKey);
            LOGGER.warn("google.translate.clientEmail: {}", clientEmail);
            LOGGER.warn("google.translate.clientId: {}", clientId);
        }
    }

    public String translate(String text, String sourceLanguageCode, String targetLanguageCode) throws IOException {
        return withTranslationClient(translationServiceClient -> {
            List<Translation> translations = translationServiceClient.translateText(LocationName.of(project, location),
                    "", "", sourceLanguageCode, targetLanguageCode, List.of(text)).getTranslationsList();
            if (translations.size() != 1) {
                throw new IllegalStateException(translations.toString());
            }
            return StringEscapeUtils.unescapeXml(translations.get(0).getTranslatedText());
        });
    }

    private String withTranslationClient(Function<TranslationServiceClient, String> function) throws IOException {
        try (TranslationServiceClient translationServiceClient = TranslationServiceClient
                .create(TranslationServiceSettings.newBuilder()
                        .setCredentialsProvider(FixedCredentialsProvider.create(ServiceAccountCredentials.newBuilder()
                                .setProjectId(project).setPrivateKeyId(privateKeyId).setPrivateKeyString(privateKey)
                                .setClientEmail(clientEmail).setClientId(clientId).build()))
                        .build())) {
            return function.apply(translationServiceClient);
        }
    }
}
