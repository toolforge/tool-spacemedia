package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.noirlab.NOIRLabMediaRepository;

@SpringJUnitConfig(NOIRLabServiceTest.TestConfig.class)
class NOIRLabServiceTest extends AbstractAgencyServiceTest {

    @MockBean
    private NOIRLabMediaRepository repository;

    @Autowired
    private NOIRLabService service;

    @Test
    void testReadHtmlTime1() throws Exception {
        // Dec. 5, 2022, 10:25 a.m.
        assertEquals(
                "NOIRLabMedia [id=360Pano_NicholasU-Mayall_4m_Telescope-CC-FD, imageType=Photographic, date=2022-12-05T10:25, width=11051, height=11051, objectName=Nicholas U. Mayall 4-meter Telescope, objectType=[], objectCategories=[Fulldome, Kitt Peak National Observatory], credit=NOIRLab/NSF/AURA/T. Slovinský, fullResMetadata=Metadata [assetUrl=https://noirlab.edu/public/media/archives/images/original/360Pano_NicholasU-Mayall_4m_Telescope-CC-FD.tif, ], sha1=Metadata [assetUrl=https://noirlab.edu/public/media/archives/images/large/360Pano_NicholasU-Mayall_4m_Telescope-CC-FD.jpg, ], title=Nicholas U. Mayall 4-meter Telescope Fulldome, description=A fulldome view of the Nicholas U. Mayall 4-meter Telescope at Kitt Peak National Observatory in Arizona.A 360 panorama version of this image can be viewed <a href=\"https://noirlab.edu/public/images/360Pano_NicholasU-Mayall_4m_Telescope-CC/\">here</a>., commonsFileNames=[], ]",
                service.newMediaFromHtml(html("noirlab/Nicholas_U_Mayall_4-meter_Telescope_Fulldome_NOIRLab.htm"),
                                new URL("https://noirlab.edu/public/images/360Pano_NicholasU-Mayall_4m_Telescope-CC-FD/"),
                                "360Pano_NicholasU-Mayall_4m_Telescope-CC-FD", null)
                        .toString());
    }

    @Test
    void testReadHtmlTime2() throws Exception {
        // March 2, 2023, 10:07 a.m.
        assertEquals(
                "NOIRLabMedia [id=Roll_off_Roof_Observatory_pic_8-CC, imageType=Photographic, date=2023-03-02T10:07, width=5426, height=3322, objectName=Visitor Center Roll Off Roof Observatory, objectType=[], objectCategories=[Kitt Peak National Observatory], credit=KPNO/NOIRLab/NSF/AURA/T. Matsopoulos, fullResMetadata=Metadata [assetUrl=https://noirlab.edu/public/media/archives/images/original/Roll_off_Roof_Observatory_pic_8-CC.tif, ], sha1=Metadata [assetUrl=https://noirlab.edu/public/media/archives/images/large/Roll_off_Roof_Observatory_pic_8-CC.jpg, ], title=Kitt Peak Visitor Center Roll Off Roof Observatory 0.4-meter Telescope, description=The 0.4-meter Telescope inside the&nbsp;<a href=\"https://noirlab.edu/public/programs/kitt-peak-national-observatory/visitor-center-roll-off-roof/\">Kitt Peak Visitor Center Roll Off Roof Observatory</a>, located at Kitt Peak National Observatory (<a href=\"https://noirlab.edu/public/programs/kitt-peak-national-observatory\">KPNO</a>), a Program of NSF's NOIRLab., commonsFileNames=[], ]",
                service.newMediaFromHtml(html("noirlab/Roll_off_Roof_Observatory_pic_8-CC.htm"),
                        new URL("https://noirlab.edu/public/images/Roll_off_Roof_Observatory_pic_8-CC/"),
                        "Roll_off_Roof_Observatory_pic_8-CC", null).toString());
    }

    @Test
    void testReadHtmlTime3() throws Exception {
        // March 1, 2023, 11 a.m.
        assertEquals(
                "NOIRLabMedia [id=noirlab2307a, imageType=Observation, date=2023-03-01T11:00, width=13546, height=10647, objectName=RCW 86, objectType=[], objectCategories=[Stars], credit=CTIO/NOIRLab/DOE/NSF/AURA T.A. Rector (University of Alaska Anchorage/NSF’s NOIRLab), J. Miller (Gemini Observatory/NSF’s NOIRLab), M. Zamani & D. de Martin (NSF’s NOIRLab), fullResMetadata=Metadata [assetUrl=https://noirlab.edu/public/media/archives/images/original/noirlab2307a.tif, ], sha1=Metadata [assetUrl=https://noirlab.edu/public/media/archives/images/large/noirlab2307a.jpg, ], title=DECam Images RCW 86, Remains of Supernova Witnessed in 185, description=The tattered shell of the first-ever recorded supernova was captured by the US Department of Energy-fabricated Dark Energy Camera, which is mounted on the National Science Foundation’s (NSF) <a href=\"https://noirlab.edu/public/programs/ctio/victor-blanco-4m-telescope/\">Víctor M. Blanco 4-meter Telescope</a> at <a href=\"https://noirlab.edu/public/programs/ctio/\">Cerro Tololo Inter-American Observatory</a> in Chile, a Program of NSF’s NOIRLab. A ring of glowing debris is all that remains of a white dwarf star that exploded more than 1800 years ago when it was recorded by Chinese astronomers as a ‘guest star’. This special image, which covers an impressive 45 arcminutes on the sky, gives a rare view of the entirety of this supernova remnant., commonsFileNames=[], ]",
                service.newMediaFromHtml(html("noirlab/noirlab2307a.htm"),
                                new URL("https://noirlab.edu/public/images/noirlab2307a/"), "noirlab2307a", null)
                        .toString());
    }

    @Test
    void testReadHtmlTime4() throws Exception {
        // Feb. 9, 2021, noon
        assertEquals(
                "NOIRLabMedia [id=ann20019a, imageType=Artwork, date=2021-02-09T12:00, width=801, height=801, objectType=[], objectCategories=[Illustrations], credit=Rubin Observatory/NSF/AURA, fullResMetadata=Metadata [assetUrl=https://noirlab.edu/public/media/archives/images/original/ann20019a.tif, ], sha1=Metadata [assetUrl=https://noirlab.edu/public/media/archives/images/large/ann20019a.jpg, ], title=Rubin Observatory Logo, description=Rubin Observatory Logo, commonsFileNames=[], ]",
                service.newMediaFromHtml(html("noirlab/ann20019a.htm"),
                new URL("https://noirlab.edu/public/images/ann20019a/"), "ann20019a", null).toString());
    }

    @Test
    void testReadHtmlTime5() throws Exception {
        // April 5, 2023, noon
        assertEquals(
                "NOIRLabMedia [id=iotw2314a, imageType=Photographic, date=2023-04-05T12:00, width=6226, height=4151, objectName=McMath-Pierce Solar Telescope, objectType=[], objectCategories=[Kitt Peak National Observatory], credit=KPNO/NOIRLab/NSF/AURA/P. Horálek (Institute of Physics in Opava), fullResMetadata=Metadata [assetUrl=https://noirlab.edu/public/media/archives/images/original/iotw2314a.tif, ], sha1=Metadata [assetUrl=https://noirlab.edu/public/media/archives/images/large/iotw2314a.jpg, ], title=The Belt of Venus over the McMath-Pierce Solar Telescope, description=The <a href=\"https://noirlab.edu/public/programs/kitt-peak-national-observatory/mcmath-pierce-solar-telescope/\">McMath-Pierce Solar Telescope</a>, located at Kitt Peak National Observatory (<a href=\"https://noirlab.edu/public/programs/kitt-peak-national-observatory/\">KPNO</a>), a Program of NSF’s NOIRLab, is captured here beneath the full moon just after sunset. This is the perfect time of day to witness a phenomenon known as the anti-twilight arch, nicknamed the <a href=\"https://en.wikipedia.org/wiki/Belt_of_Venus\">Belt of Venus</a>. The belt forms directly opposite the rising or setting Sun — in this image, the Sun is setting in the west behind the camera. Rays of light from the Sun hit the eastern atmosphere at the <a href=\"https://en.wikipedia.org/wiki/Antisolar_point\">antisolar point</a>, the point directly opposite the sun from an observer’s perspective. The light is then backscattered off of the atmosphere and reflected back to the observer at a longer wavelength, changing the typically blue-appearing light into pink. The band of dark blue sky below the anti-twilight arch is actually the Earth’s shadow!You can find a diagram representation of this phenomenon <a href=\"https://www.skyatnightmagazine.com/advice/belt-of-venus/\">here</a>.This photo was taken as part of the recent <a href=\"https://www.instagram.com/p/Cb-7Y5SPUKi/?utm_source=ig_web_copy_link\">NOIRLab 2022 Photo Expedition</a> to all the NOIRLab sites., commonsFileNames=[], ]",
                service.newMediaFromHtml(html("noirlab/iotw2314a.htm"),
                new URL("https://noirlab.edu/public/images/iotw2314a/"), "iotw2314a", null).toString());
    }

    @Test
    void testReadHtmlDate() throws Exception {
        // July 21, 2017
        assertEquals(
                "NOIRLabMedia [id=noaoann17007a, imageType=Collage, date=2017-07-21T00:00, width=848, height=400, objectType=[], objectCategories=[Galaxies], credit=Observers: D. Gerdes and S. Jouvel; Inset Image Credit: T. Abbott and NOAO/AURA/<a href=\"https://www.nsf.gov/\">NSF</a>, fullResMetadata=Metadata [assetUrl=https://noirlab.edu/public/media/archives/images/original/noaoann17007a.tif, ], sha1=Metadata [assetUrl=https://noirlab.edu/public/media/archives/images/large/noaoann17007a.jpg, ], title=Superluminous supernova proclaims the death of a star at cosmic high noon, description=Ten billion years ago, a massive star ended its life in a brilliant explosion three times as bright as all of the stars in our galaxy, the Milky Way, combined. News of its death, which recently reached Earth, was detected in the Dark Energy Survey being carried out with DECam at the CTIO Blanco telescope (pictured, above right). The supernova is one of the most distant ever discovered and confirmed., commonsFileNames=[], ]",
                service.newMediaFromHtml(html("noirlab/noaoann17007a.htm"),
                                new URL("https://noirlab.edu/public/images/noaoann17007a/"), "noaoann17007a", null)
                        .toString());
    }

    @Configuration
    @Import(DefaultAgencyTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public NOIRLabService service(NOIRLabMediaRepository repository,
                @Value("${noirlab.search.link}") String searchLink) {
            return new NOIRLabService(repository, searchLink);
        }
    }
}
