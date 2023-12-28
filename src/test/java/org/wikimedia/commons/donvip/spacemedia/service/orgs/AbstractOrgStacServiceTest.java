package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.AbstractOrgStacService.StacItem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class AbstractOrgStacServiceTest {

    @Test
    void testJsonDeserialisation() throws Exception {
        String string = new ObjectMapper().registerModules(new Jdk8Module(), new JavaTimeModule())
                .readValue(
                        new File("src/test/resources/capella/CAPELLA_C09_SM_GEC_VV_20231118132210_20231118132214.json"),
                        StacItem.class)
                .toString();
        assertTrue(string.startsWith(
                "StacItem[type=Feature, stacVersion=1.0.0, id=CAPELLA_C09_SM_GEC_VV_20231118132210_20231118132214, properties=StacProperties[datetime=2023-11-18T13:22:12.698788Z, startDatetime=2023-11-18T13:22:10.528436Z, endDatetime=2023-11-18T13:22:14.869140Z, platform=capella-9, constellation=capella, instruments=[capella-radar-9], projCentroid=[-115.19457753, 32.906081959999995], projEpsg=32611, projShape=[20211, 27193], projTransform=[0.8, 0.0, 657965.5598879671, 0.0, -0.8, 3650407.901766273, 0.0, 0.0, 1.0], sarCenterFrequency=9.65, sarFrequencyBand=X, sarInstrumentMode=stripmap, sarLooksAzimuth=1, sarLooksEquivalentNumber=1, sarLooksRange=1, sarObservationDirection=right, sarPixelSpacingAzimuth=0.8, sarPixelSpacingRange=0.8, sarPolarizations=[VV], sarProductType=GEC, sarResolutionAzimuth=1.27, sarResolutionRange=0.95, satOrbitState=ascending, viewIncidenceAngle=33.7, viewLookAngle=30.5], geometry=StacGeometry[type=Polygon, coordinates=[[[-115.30064093, 32.8831782], [-115.27502894, 32.84335589], [-115.08851413, 32.92893387], [-115.11405004, 32.96880803], [-115.30064093, 32.8831782]]]], links=[StacLink[rel=root, href=../../../catalog.json, type=application/json, title=Capella Open Data], StacLink[rel=collection, href=../collection.json, type=application/json, title=Other], StacLink[rel=parent, href=../collection.json, type=application/json, title=Other]], assets=StacAssets[HH=null, VV=StacAsset[href=https://capella-open-data.s3.amazonaws.com/data/2023/11/18/CAPELLA_C09_SM_GEC_VV_20231118132210_20231118132214/CAPELLA_C09_SM_GEC_VV_20231118132210_20231118132214.tif, type=image/tiff; application=geotiff, title=Data file, sarPolarizations=[VV], roles=[data]], HV=null, VH=null, metadata=StacAsset[href=https://capella-open-data.s3.amazonaws.com/data/2023/11/18/CAPELLA_C09_SM_GEC_VV_20231118132210_20231118132214/CAPELLA_C09_SM_GEC_VV_20231118132210_20231118132214_extended.json, type=application/json, title=Extended Metadata, sarPolarizations=null, roles=[metadata]], preview=StacAsset[href=https://capella-open-data.s3.amazonaws.com/data/2023/11/18/CAPELLA_C09_SM_GEO_VV_20231118132210_20231118132214/CAPELLA_C09_SM_GEO_VV_20231118132210_20231118132214_preview.tif, type=image/tiff; application=geotiff; profile=cloud-optimized, title=Preview image, sarPolarizations=null, roles=[overview]], thumbnail=StacAsset[href=https://capella-open-data.s3.amazonaws.com/data/2023/11/18/CAPELLA_C09_SM_GEO_VV_20231118132210_20231118132214/CAPELLA_C09_SM_GEO_VV_20231118132210_20231118132214_thumb.png, type=image/png, title=Thumbnail image, sarPolarizations=null, roles=[thumbnail]]], bbox=["));
        assertTrue(string.endsWith(
                ", stacExtensions=[sat, view, sar, processing, proj], collection=capella-open-data-other]"));
    }
}
