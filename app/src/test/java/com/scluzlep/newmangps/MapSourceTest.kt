package com.scluzlep.newmangps

import com.scluzlep.newmangps.data.MapCoordSystem
import com.scluzlep.newmangps.data.MapSource
import com.scluzlep.newmangps.data.api.AmapPlace
import com.scluzlep.newmangps.data.util.CoordinateTransform
import com.scluzlep.newmangps.ui.screens.map.MapProjection
import com.scluzlep.newmangps.ui.screens.map.bingQuadKey
import com.scluzlep.newmangps.ui.screens.map.buildTileLayerSpecs
import com.scluzlep.newmangps.ui.screens.map.isValidMapCoordinate
import com.scluzlep.newmangps.ui.screens.map.tmsY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MapSourceTest {

    @Test
    fun allSevenSourcesHaveExpectedMetadata() {
        assertEquals(7, MapSource.entries.size)
        assertEquals(
            listOf("amap", "tencent", "tianmap", "bing", "arcgis", "osm", "google"),
            MapSource.entries.map { it.id }
        )

        assertEquals(MapCoordSystem.GCJ02, MapSource.AMAP.coordSystem)
        assertTrue(MapSource.AMAP.supportsSatellite)
        assertTrue(MapSource.AMAP.supportsTraffic)
        assertEquals(18, MapSource.AMAP.maxZoom)

        assertEquals(MapCoordSystem.GCJ02, MapSource.TENCENT.coordSystem)
        assertTrue(MapSource.TENCENT.supportsSatellite)
        assertFalse(MapSource.TENCENT.supportsTraffic)
        assertEquals(18, MapSource.TENCENT.maxZoom)

        assertEquals(MapCoordSystem.WGS84, MapSource.TIANMAP.coordSystem)
        assertTrue(MapSource.TIANMAP.supportsSatellite)
        assertFalse(MapSource.TIANMAP.supportsTraffic)
        assertEquals(18, MapSource.TIANMAP.maxZoom)

        assertEquals(MapCoordSystem.GCJ02, MapSource.BING.coordSystem)
        assertTrue(MapSource.BING.supportsSatellite)
        assertFalse(MapSource.BING.supportsTraffic)
        assertEquals(18, MapSource.BING.maxZoom)

        assertEquals(MapCoordSystem.WGS84, MapSource.ARCGIS.coordSystem)
        assertTrue(MapSource.ARCGIS.supportsSatellite)
        assertFalse(MapSource.ARCGIS.supportsTraffic)
        assertEquals(18, MapSource.ARCGIS.maxZoom)

        assertEquals(MapCoordSystem.WGS84, MapSource.OSM.coordSystem)
        assertFalse(MapSource.OSM.supportsSatellite)
        assertFalse(MapSource.OSM.supportsTraffic)
        assertEquals(19, MapSource.OSM.maxZoom)

        assertEquals(MapCoordSystem.GCJ02, MapSource.GOOGLE.coordSystem)
        assertTrue(MapSource.GOOGLE.supportsSatellite)
        assertFalse(MapSource.GOOGLE.supportsTraffic)
        assertEquals(19, MapSource.GOOGLE.maxZoom)
    }

    @Test
    fun fromIdResolvesAllSourcesAndFallsBackToAmap() {
        MapSource.entries.forEach { source ->
            assertEquals(source, MapSource.fromId(source.id))
        }
        assertEquals(MapSource.AMAP, MapSource.fromId(null))
        assertEquals(MapSource.AMAP, MapSource.fromId(""))
        assertEquals(MapSource.AMAP, MapSource.fromId("baidu"))
    }

    @Test
    fun coordinateRoundTripsAcrossAllSources() {
        val beijingWgs = CoordinateTransform.LatLng(39.9042, 116.4074)
        val londonWgs = CoordinateTransform.LatLng(51.5074, -0.1278)

        MapSource.entries.forEach { source ->
            val mappedChina = source.toMapCoord(beijingWgs)
            val roundTripChina = source.fromMapCoord(mappedChina)
            if (source.coordSystem == MapCoordSystem.WGS84) {
                assertEquals(beijingWgs, mappedChina)
                assertEquals(beijingWgs, roundTripChina)
            } else {
                assertNotEquals(beijingWgs, mappedChina)
                assertTrue(abs(mappedChina.latitude - beijingWgs.latitude) > 0.0005)
                assertTrue(abs(mappedChina.longitude - beijingWgs.longitude) > 0.001)
                assertEquals(beijingWgs.latitude, roundTripChina.latitude, 1e-4)
                assertEquals(beijingWgs.longitude, roundTripChina.longitude, 1e-4)
            }

            // Outside China, GCJ-02 and WGS-84 are identical
            val mappedForeign = source.toMapCoord(londonWgs)
            assertEquals(londonWgs.latitude, mappedForeign.latitude, 1e-9)
            assertEquals(londonWgs.longitude, mappedForeign.longitude, 1e-9)
            val roundTripForeign = source.fromMapCoord(mappedForeign)
            assertEquals(londonWgs.latitude, roundTripForeign.latitude, 1e-9)
            assertEquals(londonWgs.longitude, roundTripForeign.longitude, 1e-9)
        }
    }

    @Test
    fun amapPlaceWgsPointConvertsFromGcj02ToWgs84() {
        val wgs = CoordinateTransform.LatLng(22.5431, 114.0579)
        val gcj = CoordinateTransform.wgs84ToGcj02(wgs.latitude, wgs.longitude)
        val place = AmapPlace("Test", "Shenzhen", gcj)
        assertEquals(wgs.latitude, place.wgsPoint.latitude, 1e-4)
        assertEquals(wgs.longitude, place.wgsPoint.longitude, 1e-4)
    }

    @Test
    fun mercatorProjectionRoundTripsAndMetersScaleCorrectly() {
        val samples = listOf(
            CoordinateTransform.LatLng(0.0, 0.0),
            CoordinateTransform.LatLng(39.9042, 116.4074),
            CoordinateTransform.LatLng(-33.8688, 151.2093),
            CoordinateTransform.LatLng(64.1466, -21.9426)
        )
        val tileSizePx = 768.0 // 3x density
        for (zoom in listOf(2.0, 5.5, 15.0, 18.25)) {
            for (pt in samples) {
                val (wx, wy) = MapProjection.latLonToWorldPx(pt.latitude, pt.longitude, zoom, tileSizePx)
                val restored = MapProjection.worldPxToLatLon(wx, wy, zoom, tileSizePx)
                assertEquals(pt.latitude, restored.latitude, 1e-6)
                assertEquals(pt.longitude, restored.longitude, 1e-6)
            }
        }

        val z15 = MapProjection.metersToPixels(500.0, 39.9042, 15.0, tileSizePx)
        val z16 = MapProjection.metersToPixels(500.0, 39.9042, 16.0, tileSizePx)
        assertTrue(z15 > 0.0)
        assertEquals(z15 * 2.0, z16, 1e-6)
        assertEquals(0.0, MapProjection.metersToPixels(-10.0, 0.0, 10.0, tileSizePx), 1e-9)
    }

    @Test
    fun fitBoundsZoomHandlesSinglePointMultiPointAndPadding() {
        val tileSizePx = 768.0
        val (singleCenter, singleZoom) = MapProjection.fitBoundsZoom(
            minLat = 39.9042,
            maxLat = 39.9042,
            minLon = 116.4074,
            maxLon = 116.4074,
            viewportWidthPx = 1080.0,
            viewportHeightPx = 1800.0,
            tileSizePx = tileSizePx,
            currentZoom = 5.0
        )
        assertEquals(39.9042, singleCenter.latitude, 1e-6)
        assertEquals(116.4074, singleCenter.longitude, 1e-6)
        assertEquals(15.0, singleZoom, 1e-6)

        val (multiCenter, multiZoom) = MapProjection.fitBoundsZoom(
            minLat = 39.85,
            maxLat = 39.95,
            minLon = 116.30,
            maxLon = 116.50,
            viewportWidthPx = 1080.0,
            viewportHeightPx = 1800.0,
            padLeft = 60.0,
            padTop = 200.0,
            padRight = 60.0,
            padBottom = 400.0,
            tileSizePx = tileSizePx,
            currentZoom = 5.0
        )
        assertTrue(multiZoom in 10.0..17.0)
        val (camWx, camWy) = MapProjection.latLonToWorldPx(
            multiCenter.latitude,
            multiCenter.longitude,
            multiZoom,
            tileSizePx
        )
        val (nwX, nwY) = MapProjection.latLonToWorldPx(39.95, 116.30, multiZoom, tileSizePx)
        val (seX, seY) = MapProjection.latLonToWorldPx(39.85, 116.50, multiZoom, tileSizePx)
        val screenMinX = 1080.0 / 2.0 + (nwX - camWx)
        val screenMaxX = 1080.0 / 2.0 + (seX - camWx)
        val screenMinY = 1800.0 / 2.0 + (nwY - camWy)
        val screenMaxY = 1800.0 / 2.0 + (seY - camWy)
        assertTrue(screenMinX >= 59.0 && screenMaxX <= 1021.0)
        assertTrue(screenMinY >= 199.0 && screenMaxY <= 1401.0)
    }

    @Test
    fun tileUrlBuildersAndCoordinateValidationCoverAllSources() {
        assertEquals("0", bingQuadKey(1, 0, 0))
        assertEquals("3", bingQuadKey(1, 1, 1))
        assertEquals("13210", bingQuadKey(5, 26, 12))
        assertEquals(19, tmsY(5, 12))

        val amapStd = buildTileLayerSpecs(MapSource.AMAP, satellite = false, traffic = true)
        assertEquals(2, amapStd.size)
        assertFalse(amapStd[0].isOptionalOverlay)
        assertTrue(amapStd[0].urlFor(15, 26978, 12422).contains("webrd0"))
        assertTrue(amapStd[0].urlFor(15, 26978, 12422).contains("style=8"))
        assertTrue(amapStd[1].isOptionalOverlay)
        assertEquals(7, amapStd[1].minZoom)
        assertTrue(amapStd[1].urlFor(15, 26978, 12422).contains("traffictile"))

        val amapSat = buildTileLayerSpecs(MapSource.AMAP, satellite = true, traffic = false)
        assertEquals(2, amapSat.size)
        assertTrue(amapSat[0].urlFor(15, 26978, 12422).contains("style=6"))
        assertTrue(amapSat[1].isOptionalOverlay)
        assertTrue(amapSat[1].urlFor(15, 26978, 12422).contains("scl=1"))

        val tencentSat = buildTileLayerSpecs(MapSource.TENCENT, satellite = true, traffic = false)
        assertEquals(2, tencentSat.size)
        assertTrue(tencentSat[0].urlFor(5, 26, 12).endsWith("/26_19.jpg"))
        assertTrue(tencentSat[1].isOptionalOverlay)
        assertTrue(tencentSat[1].urlFor(5, 26, 12).contains("y=19"))

        val tianmap = buildTileLayerSpecs(
            MapSource.TIANMAP,
            satellite = false,
            traffic = false,
            tianmapKey = "test key+1"
        )
        assertEquals(2, tianmap.size)
        assertTrue(tianmap[0].urlFor(5, 26, 12).contains("vec_w/wmts"))
        assertTrue(tianmap[0].urlFor(5, 26, 12).contains("tk=test+key%2B1"))
        assertTrue(tianmap[1].isOptionalOverlay)
        assertTrue(tianmap[1].urlFor(5, 26, 12).contains("cva_w/wmts"))

        val bingSat = buildTileLayerSpecs(MapSource.BING, satellite = true, traffic = false)
        assertEquals(1, bingSat.size)
        assertTrue(bingSat[0].urlFor(5, 26, 12).contains("/tiles/h13210.jpeg"))

        val arcgisSat = buildTileLayerSpecs(MapSource.ARCGIS, satellite = true, traffic = false)
        assertEquals(2, arcgisSat.size)
        assertTrue(arcgisSat[0].urlFor(5, 26, 12).contains("World_Imagery/MapServer/tile/5/12/26"))
        assertTrue(arcgisSat[1].isOptionalOverlay)

        val osm = buildTileLayerSpecs(MapSource.OSM, satellite = false, traffic = false)
        assertEquals(1, osm.size)
        assertEquals(19, osm[0].maxZoom)
        assertEquals("https://tile.openstreetmap.org/5/26/12.png", osm[0].urlFor(5, 26, 12))

        val googleSat = buildTileLayerSpecs(MapSource.GOOGLE, satellite = true, traffic = false)
        assertEquals(1, googleSat.size)
        assertTrue(googleSat[0].urlFor(5, 26, 12).contains("lyrs=y"))

        assertTrue(isValidMapCoordinate(39.9042, 116.4074))
        assertFalse(isValidMapCoordinate(Double.NaN, 116.0))
        assertFalse(isValidMapCoordinate(89.0, 116.0))
        assertFalse(isValidMapCoordinate(39.0, 181.0))
    }
}
