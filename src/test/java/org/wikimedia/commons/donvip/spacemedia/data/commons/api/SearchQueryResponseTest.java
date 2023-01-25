package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class SearchQueryResponseTest {

    @Test
    void testJsonDeserialization() throws Exception {
        // https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrlimit=10&gsroffset=0&gsrinfo=totalhits&gsrsearch=filetype%3Abitmap|drawing-fileres%3A0%20NHQ202206070021&prop=info|imageinfo|entityterms&inprop=url&gsrnamespace=6&iiprop=url|size|mime&format=json
        String json = "{\"batchcomplete\":\"\",\"query\":{\"searchinfo\":{\"totalhits\":1},\"pages\":{\"119064333\":{\"pageid\":119064333,\"ns\":6,\"title\":\"File:France Artemis Accords Signing Ceremony Nhq202206070021 52130395523.jpg\",\"index\":1,\"contentmodel\":\"wikitext\",\"pagelanguage\":\"en\",\"pagelanguagehtmlcode\":\"en\",\"pagelanguagedir\":\"ltr\",\"touched\":\"2022-12-23T01:44:42Z\",\"lastrevid\":718887783,\"length\":971,\"fullurl\":\"https://commons.wikimedia.org/wiki/File:France_Artemis_Accords_Signing_Ceremony_Nhq202206070021_52130395523.jpg\",\"editurl\":\"https://commons.wikimedia.org/w/index.php?title=File:France_Artemis_Accords_Signing_Ceremony_Nhq202206070021_52130395523.jpg&action=edit\",\"canonicalurl\":\"https://commons.wikimedia.org/wiki/File:France_Artemis_Accords_Signing_Ceremony_Nhq202206070021_52130395523.jpg\",\"imagerepository\":\"local\",\"imageinfo\":[{\"size\":2351236,\"width\":2400,\"height\":1600,\"url\":\"https://upload.wikimedia.org/wikipedia/commons/5/54/France_Artemis_Accords_Signing_Ceremony_Nhq202206070021_52130395523.jpg\",\"descriptionurl\":\"https://commons.wikimedia.org/wiki/File:France_Artemis_Accords_Signing_Ceremony_Nhq202206070021_52130395523.jpg\",\"descriptionshorturl\":\"https://commons.wikimedia.org/w/index.php?curid=119064333\",\"mime\":\"image/jpeg\"}]}}}}";
        assertNotNull(new ObjectMapper().registerModules(new Jdk8Module(), new JavaTimeModule()).readValue(json,
                SearchQueryResponse.class));
    }
}
