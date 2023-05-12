package org.wikimedia.commons.donvip.spacemedia.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wikimedia.commons.donvip.spacemedia.controller.SpaceAgencyRestControllerTest.TestSpaceAgencyRestController;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.AbstractAgencyService;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.AsyncAgencyUpdaterService;

@WebMvcTest(TestSpaceAgencyRestController.class)
@ContextConfiguration(classes = SpaceAgencyRestControllerTest.Config.class)
class SpaceAgencyRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestSpaceAgencyRestController controller;

    @MockBean
    private AsyncAgencyUpdaterService async;

    @MockBean
    private AbstractAgencyService<Media<String, LocalDate>, String, LocalDate> service;

    @Test
    void testExtractId() {
        assertEquals("foo", controller.extractId(mockHttp("foo"), "test"));
        assertEquals("foo/bar", controller.extractId(mockHttp("foo/bar"), "test"));
        assertEquals("foo/bar/baz", controller.extractId(mockHttp("foo/bar/baz"), "test"));
    }

    @Test
    void shouldWololoReturn404() throws Exception {
        mockMvc.perform(get("/snoopy/rest/wololo")).andDo(print()).andExpect(status().is(404));
    }

    @Test
    void shouldUpdateReturnOk() throws Exception {
        mockMvc.perform(get("/snoopy/rest/update")).andDo(print()).andExpect(status().isOk());
    }

    @Test
    void shouldUploadMediaWithoutSlashReturnOk() throws Exception {
        mockMvc.perform(get("/snoopy/rest/uploadmedia/foo")).andDo(print()).andExpect(status().isOk());
    }

    @Test
    void shouldUploadMediaWithSlashReturnOk() throws Exception {
        mockMvc.perform(get("/snoopy/rest/uploadmedia/foo/bar")).andDo(print()).andExpect(status().isOk());
    }

    @Test
    void shouldUploadMediaWithSlashesReturnOk() throws Exception {
        mockMvc.perform(get("/snoopy/rest/uploadmedia/foo/bar/baz")).andDo(print()).andExpect(status().isOk());
    }

    private static MockHttpServletRequest mockHttp(String uri) {
        return new MockHttpServletRequest("GET", "/snoopy/rest/test/" + uri);
    }

    @Configuration
    static class Config {

        @Bean
        @Autowired
        public TestSpaceAgencyRestController controller(
                AbstractAgencyService<Media<String, LocalDate>, String, LocalDate> service) {
            return new TestSpaceAgencyRestController(service);
        }
    }

    @RestController
    @RequestMapping(path = "snoopy/rest")
    static class TestSpaceAgencyRestController
            extends SpaceAgencyRestController<Media<String, LocalDate>, String, LocalDate> {

        public TestSpaceAgencyRestController(
                AbstractAgencyService<Media<String, LocalDate>, String, LocalDate> service) {
            super(service);
        }
    }
}
