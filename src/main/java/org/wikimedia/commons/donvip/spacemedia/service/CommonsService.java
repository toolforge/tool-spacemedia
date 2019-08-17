package org.wikimedia.commons.donvip.spacemedia.service;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsCategoryRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsImage;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsImageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsOldImage;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsOldImageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsPageRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.CategoryNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.CategoryPageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CommonsService {

    @Autowired
    private CommonsImageRepository commonsImageRepository;

    @Autowired
    private CommonsOldImageRepository commonsOldImageRepository;

    @Autowired
    private CommonsCategoryRepository commonsCategoryRepository;

    @Autowired
    private CommonsPageRepository commonsPageRepository;

    @Value("${commons.api.url}")
    private URL commonsApiUrl;

    public Set<String> findFilesWithSha1(String sha1) {
        // See https://www.mediawiki.org/wiki/Manual:Image_table#img_sha1
        // The SHA-1 hash of the file contents in base 36 format, zero-padded to 31 characters 
        String sha1base36 = String.format("%31s", new BigInteger(sha1, 16).toString(36)).replace(' ', '0');
        Set<String> files = commonsImageRepository.findBySha1(sha1base36).stream().map(CommonsImage::getName).collect(Collectors.toSet());
        if (files.isEmpty()) {
            files.addAll(commonsOldImageRepository.findBySha1(sha1base36).stream().map(CommonsOldImage::getName).collect(Collectors.toSet()));
        }
        return files;
    }

    public String getWikiHtmlPreview(String wikiCode, String pageTitle) throws ClientProtocolException, IOException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(commonsApiUrl.toExternalForm());
            List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("action", "visualeditor"));
            nvps.add(new BasicNameValuePair("format", "json"));
            nvps.add(new BasicNameValuePair("formatversion", "2"));
            nvps.add(new BasicNameValuePair("paction", "parsedoc"));
            nvps.add(new BasicNameValuePair("page", pageTitle));
            nvps.add(new BasicNameValuePair("wikitext", wikiCode));
            nvps.add(new BasicNameValuePair("pst", "true"));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, StandardCharsets.UTF_8));
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
                HttpEntity entity = response.getEntity();
                Header encoding = entity.getContentEncoding();
                String body = IOUtils.toString(entity.getContent(),
                        encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding.getValue()));
                EntityUtils.consume(entity);
                VisualEditorResponse apiResponse = new ObjectMapper().readValue(body, ApiResponse.class)
                        .getVisualeditor();
                if (!"success".equals(apiResponse.getResult())) {
                    throw new IllegalArgumentException(apiResponse.toString());
                }
                return apiResponse.getContent();
            }
        }
    }

    @SuppressWarnings("serial")
    public String getWikiHtmlPreview(String wikiCode, String pageTitle, String imgUrl)
            throws ClientProtocolException, IOException {
        Document doc = Jsoup.parse(getWikiHtmlPreview(wikiCode, pageTitle));
        Element body = doc.getElementsByTag("body").get(0);
        // Display image
        Element imgLink = Utils.prependChildElement(body, "a", null, new HashMap<String, String>() {
            {
                put("href", imgUrl);
            }
        });
        Utils.appendChildElement(imgLink, "img", null, new HashMap<String, String>() {
            {
                put("src", imgUrl);
                put("width", "800");
            }
        });
        // Display categories
        Element lastSection = body.getElementsByTag("section").last();
        Element catLinksDiv = Utils.appendChildElement(lastSection, "div", null, new HashMap<String, String>() {
            {
                put("id", "catlinks");
                put("class", "catlinks");
                put("data-mw", "interface");
            }
        });
        Element normalCatLinksDiv = Utils.appendChildElement(catLinksDiv, "div", null, new HashMap<String, String>() {
            {
                put("id", "mw-normal-catlinks");
                put("class", "mw-normal-catlinks");
            }
        });
        Utils.appendChildElement(normalCatLinksDiv, "a", "Categories", new HashMap<String, String>() {
            {
                put("href", "https://commons.wikimedia.org/wiki/Special:Categories");
                put("title", "Special:Categories");
            }
        });
        normalCatLinksDiv.appendText(": ");
        Element normalCatLinksList = new Element("ul");
        normalCatLinksDiv.appendChild(normalCatLinksList);
        Element hiddenCatLinksList = new Element("ul");
        Utils.appendChildElement(catLinksDiv, "div", "Hidden categories: ", new HashMap<String, String>() {
            {
                put("id", "mw-hidden-catlinks");
                put("class", "mw-hidden-catlinks mw-hidden-cats-user-shown");
            }
        }).appendChild(hiddenCatLinksList);
        for (Element link : lastSection.getElementsByTag("link")) {
            String category = link.attr("href").replace("#" + pageTitle, "").replace("./Category:", "");
            String href = "https://commons.wikimedia.org/wiki/Category:" + category;
            Element list = isHiddenCategory(category) ? hiddenCatLinksList : normalCatLinksList;
            Element item = new Element("li");
            list.appendChild(item);
            Utils.appendChildElement(item, "a", category.replace('_', ' '), new HashMap<String, String>() {
                {
                    put("href", href);
                    put("title", "Category:" + category);
                }
            });
            link.remove();
        }
        return doc.toString();
    }

    static class ApiResponse {
        private VisualEditorResponse visualeditor;

        public VisualEditorResponse getVisualeditor() {
            return visualeditor;
        }

        public void setVisualeditor(VisualEditorResponse visualeditor) {
            this.visualeditor = visualeditor;
        }
    }

    static class VisualEditorResponse {
        private String result;
        private String content;

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        @Override
        public String toString() {
            return "VisualEditorResponse [" + (result != null ? "result=" + result + ", " : "")
                    + (content != null ? "content=" + content : "") + "]";
        }
    }

    @Cacheable("hiddenCategories")
    public boolean isHiddenCategory(String category) {
        return commonsPageRepository.findByCategoryTitle(commonsCategoryRepository
                .findByTitle(category).orElseThrow(() -> new CategoryNotFoundException(category)).getTitle())
                .orElseThrow(() -> new CategoryPageNotFoundException(category))
                .getProps().stream().anyMatch(pp -> "hiddencat".equals(pp.getPropname()));
    }

    public Set<String> cleanupCategories(Set<String> result) {
        // TODO Auto-generated method stub
        return result;
    }
}
