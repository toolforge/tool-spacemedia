package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaType;

@SpringJUnitConfig(NOIRLabServiceTest.TestConfig.class)
class NOIRLabServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private DjangoplicityMediaRepository repository;

    @Autowired
    private NOIRLabService service;

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            // Dec. 5, 2022, 10:25 a.m.
            "360Pano_NicholasU-Mayall_4m_Telescope-CC-FD|Photographic|2022-12-05T10:25|11051|11051|Nicholas U. Mayall 4-meter Telescope||Fulldome,Kitt Peak National Observatory|NOIRLab/NSF/AURA/T. Slovinský|https://noirlab.edu/public/media/archives/images/original/360Pano_NicholasU-Mayall_4m_Telescope-CC-FD.tif,https://noirlab.edu/public/media/archives/images/large/360Pano_NicholasU-Mayall_4m_Telescope-CC-FD.jpg|Nicholas U. Mayall 4-meter Telescope Fulldome|A fulldome view of the Nicholas U. Mayall 4-meter Telescope at Kitt Peak National Observatory in Arizona.A 360 panorama version of this image can be viewed <a href=\"https://noirlab.edu/public/images/360Pano_NicholasU-Mayall_4m_Telescope-CC/\">here</a>.|",
            // March 2, 2023, 10:07 a.m.
            "Roll_off_Roof_Observatory_pic_8-CC|Photographic|2023-03-02T10:07|5426|3322|Visitor Center Roll Off Roof Observatory||Kitt Peak National Observatory|KPNO/NOIRLab/NSF/AURA/T. Matsopoulos|https://noirlab.edu/public/media/archives/images/original/Roll_off_Roof_Observatory_pic_8-CC.tif,https://noirlab.edu/public/media/archives/images/large/Roll_off_Roof_Observatory_pic_8-CC.jpg|Kitt Peak Visitor Center Roll Off Roof Observatory 0.4-meter Telescope|The 0.4-meter Telescope inside the&nbsp;<a href=\"https://noirlab.edu/public/programs/kitt-peak-national-observatory/visitor-center-roll-off-roof/\">Kitt Peak Visitor Center Roll Off Roof Observatory</a>, located at Kitt Peak National Observatory (<a href=\"https://noirlab.edu/public/programs/kitt-peak-national-observatory\">KPNO</a>), a Program of NSF's NOIRLab.|",
            // March 1, 2023, 11 a.m.
            "noirlab2307a|Observation|2023-03-01T11:00|13546|10647|RCW 86||Stars|CTIO/NOIRLab/DOE/NSF/AURA T.A. Rector (University of Alaska Anchorage/NSF’s NOIRLab), J. Miller (Gemini Observatory/NSF’s NOIRLab), M. Zamani & D. de Martin (NSF’s NOIRLab)|https://noirlab.edu/public/media/archives/images/original/noirlab2307a.tif,https://noirlab.edu/public/media/archives/images/large/noirlab2307a.jpg|DECam Images RCW 86, Remains of Supernova Witnessed in 185|The tattered shell of the first-ever recorded supernova was captured by the US Department of Energy-fabricated Dark Energy Camera, which is mounted on the National Science Foundation’s (NSF) <a href=\"https://noirlab.edu/public/programs/ctio/victor-blanco-4m-telescope/\">Víctor M. Blanco 4-meter Telescope</a> at <a href=\"https://noirlab.edu/public/programs/ctio/\">Cerro Tololo Inter-American Observatory</a> in Chile, a Program of NSF’s NOIRLab. A ring of glowing debris is all that remains of a white dwarf star that exploded more than 1800 years ago when it was recorded by Chinese astronomers as a ‘guest star’. This special image, which covers an impressive 45 arcminutes on the sky, gives a rare view of the entirety of this supernova remnant.|",
            // Feb. 9, 2021, noon
            "ann20019a|Artwork|2021-02-09T12:00|801|801|||Illustrations|Rubin Observatory/NSF/AURA|https://noirlab.edu/public/media/archives/images/original/ann20019a.tif,https://noirlab.edu/public/media/archives/images/large/ann20019a.jpg|Rubin Observatory Logo|Rubin Observatory Logo|",
            // April 5, 2023, noon
            "iotw2314a|Photographic|2023-04-05T12:00|6226|4151|McMath-Pierce Solar Telescope||Kitt Peak National Observatory|KPNO/NOIRLab/NSF/AURA/P. Horálek (Institute of Physics in Opava)|https://noirlab.edu/public/media/archives/images/original/iotw2314a.tif,https://noirlab.edu/public/media/archives/images/large/iotw2314a.jpg|The Belt of Venus over the McMath-Pierce Solar Telescope|The <a href=\"https://noirlab.edu/public/programs/kitt-peak-national-observatory/mcmath-pierce-solar-telescope/\">McMath-Pierce Solar Telescope</a>, located at Kitt Peak National Observatory (<a href=\"https://noirlab.edu/public/programs/kitt-peak-national-observatory/\">KPNO</a>), a Program of NSF’s NOIRLab, is captured here beneath the full moon just after sunset. This is the perfect time of day to witness a phenomenon known as the anti-twilight arch, nicknamed the <a href=\"https://en.wikipedia.org/wiki/Belt_of_Venus\">Belt of Venus</a>. The belt forms directly opposite the rising or setting Sun — in this image, the Sun is setting in the west behind the camera. Rays of light from the Sun hit the eastern atmosphere at the <a href=\"https://en.wikipedia.org/wiki/Antisolar_point\">antisolar point</a>, the point directly opposite the sun from an observer’s perspective. The light is then backscattered off of the atmosphere and reflected back to the observer at a longer wavelength, changing the typically blue-appearing light into pink. The band of dark blue sky below the anti-twilight arch is actually the Earth’s shadow!You can find a diagram representation of this phenomenon <a href=\"https://www.skyatnightmagazine.com/advice/belt-of-venus/\">here</a>.This photo was taken as part of the recent <a href=\"https://www.instagram.com/p/Cb-7Y5SPUKi/?utm_source=ig_web_copy_link\">NOIRLab 2022 Photo Expedition</a> to all the NOIRLab sites.|",
            // Feb. 1, 2023, 9 a.m.
            "noirlab2303b|Artwork|2023-02-01T09:00|3840|3840|||Illustrations|CTIO/NOIRLab/NSF/AURA/P. Marenfeld|https://noirlab.edu/public/media/archives/images/original/noirlab2303b.tif,https://noirlab.edu/public/media/archives/images/large/noirlab2303b.jpg|Infographic: The Evolution of CPD-29 2176, a Kilonova Progenitor|This infographic illustrates the evolution of the star system CPD-29 2176, the first confirmed kilonova progenitor. Stage 1, two massive blue stars form in a binary star system. Stage 2, the larger of the two stars nears the end of its life. Stage 3, the smaller of the two stars siphons off material from its larger, more mature companion, stripping it of much of its outer atmosphere. Stage 4, the larger star forms an ultra-stripped supernova, the end-of-life explosion of a star with less of a “kick” than a more normal supernova. Stage 5, as currently observed by astronomers, the resulting neutron star from the earlier supernova begins to siphon off material from its companion, turning the tables on the binary pair. Stage 6, with the loss of much of its outer atmosphere, the companion star also undergoes an ultra-stripped supernova. This stage will happen in about one million years. Stage 7, a pair of neutron stars in close mutual orbit now remain where once there were two massive stars. Stage 8, the two neutron stars spiral into toward each other, giving up their orbital energy as faint gravitational radiation. Stage 9, the final stage of this system as both neutron stars collide, producing a powerful kilonova, the cosmic factory of heavy elements in our Universe.&nbsp;&nbsp;|",
            // July 21, 2017
            "noaoann17007a|Collage|2017-07-21T00:00|848|400|||Galaxies|Observers: D. Gerdes and S. Jouvel; Inset Image Credit: T. Abbott and NOAO/AURA/<a href=\"https://www.nsf.gov/\">NSF</a>|https://noirlab.edu/public/media/archives/images/original/noaoann17007a.tif,https://noirlab.edu/public/media/archives/images/large/noaoann17007a.jpg|Superluminous supernova proclaims the death of a star at cosmic high noon|Ten billion years ago, a massive star ended its life in a brilliant explosion three times as bright as all of the stars in our galaxy, the Milky Way, combined. News of its death, which recently reached Earth, was detected in the Dark Energy Survey being carried out with DECam at the CTIO Blanco telescope (pictured, above right). The supernova is one of the most distant ever discovered and confirmed.|",
            // Sept. 30, 2022, 1:05 p.m.
            "GeMS-62-CC|Photographic|2022-09-30T13:05|3606|5409|Gemini Multi-Conjugate Adaptive Optics System (GeMS), Gemini South||Gemini Observatory|International Gemini Observatory/NOIRLab/NSF/AURA/M. Paredes|https://noirlab.edu/public/media/archives/images/original/GeMS-62-CC.tif,https://noirlab.edu/public/media/archives/images/large/GeMS-62-CC.jpg|Gemini South and its Instruments|The Gemini South telescope with its numerous instruments on Cerro Pachón in Chile.|",
            // Jan. 22, 2021
            "ann21004a|Artwork|2021-01-22T00:00|850|1100|||Illustrations|NOIRLab|https://noirlab.edu/public/media/archives/images/original/ann21004a.tif,https://noirlab.edu/public/media/archives/images/large/ann21004a.jpg|NOIRLab Mirror: Issue 02||",
            // <em> in credits in description
            "GeminiMontage|Collage|2022-01-04T12:00|2560|1300|||Gemini Observatory|Gemini/<a href=\"https://www.nsf.gov/\">NSF</a>/AURA|https://noirlab.edu/public/media/archives/images/original/GeminiMontage.tif,https://noirlab.edu/public/media/archives/images/large/GeminiMontage.jpg|Gemini Montage|Gemini South on the summit of Cerro Pachón in Chile (left) and Gemini North on the summit of Maunakea in Hawai’i (right)||"
    })
    void testReadHtml(String id, DjangoplicityMediaType imageType, String date, int width, int height, String name,
            @ConvertWith(SetArgumentConverter.class) Set<String> types,
            @ConvertWith(SetArgumentConverter.class) Set<String> categories, String credit,
            @ConvertWith(ListArgumentConverter.class) List<String> assetUrls, String title, String description,
            @ConvertWith(SetArgumentConverter.class) Set<String> telescopes)
            throws Exception {
        when(metadataRepository.save(any(FileMetadata.class))).thenAnswer(a -> a.getArgument(0, FileMetadata.class));
        doDjangoplicityMediaTest(service.newMediaFromHtml(html("noirlab/" + id + ".htm"),
                new URL("https://noirlab.edu/public/images/" + id + "/"), id, null), id, imageType, date,
                new MediaDimensions(width, height), name, types, categories, credit, assetUrls, title, description,
                telescopes);
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public NOIRLabService service(DjangoplicityMediaRepository repository,
                @Value("${noirlab.search.link}") String searchLink) {
            return new NOIRLabService(repository, searchLink);
        }
    }
}
