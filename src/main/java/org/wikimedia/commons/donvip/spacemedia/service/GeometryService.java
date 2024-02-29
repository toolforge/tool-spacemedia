package org.wikimedia.commons.donvip.spacemedia.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import javax.annotation.PostConstruct;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
public class GeometryService {

    private SimpleFeatureCollection continents;

    @PostConstruct
    public void postConstruct() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/World_Continents.geojson");
                GeoJSONReader reader = new GeoJSONReader(in)) {
            continents = Objects.requireNonNull(reader.getFeatures());
        }
    }

    /**
     * Returns the continent name for the given location, as defined by
     * https://hub.arcgis.com/datasets/esri::world-continents/explore?showTable=true
     *
     * @param lat latitude
     * @param lon longitude
     * @return continent name
     */
    public String getContinent(double lat, double lon) {
        Point coord = new GeometryFactory().createPoint(new Coordinate(lon, lat));
        try (SimpleFeatureIterator i = continents.features()) {
            while (i.hasNext()) {
                SimpleFeature continent = i.next();
                if (continent.getAttribute("geometry") instanceof Geometry geometry && geometry.contains(coord)) {
                    return (String) continent.getAttribute("CONTINENT");
                }
            }
        }
        return null;
    }
}
