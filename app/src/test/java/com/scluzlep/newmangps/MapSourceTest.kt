package com.scluzlep.newmangps

import com.scluzlep.newmangps.data.MapCoordSystem
import com.scluzlep.newmangps.data.MapSource
import com.scluzlep.newmangps.data.api.AmapPlace
import com.scluzlep.newmangps.data.util.CoordinateTransform
import com.scluzlep.newmangps.ui.screens.map.MapDeviceMarker
import com.scluzlep.newmangps.ui.screens.map.MapFenceOverlay
import com.scluzlep.newmangps.ui.screens.map.serializeDeviceMarkersJson
import com.scluzlep.newmangps.ui.screens.map.serializeFencesJson
import com.scluzlep.newmangps.ui.screens.map.serializeLabelsJson
import com.scluzlep.newmangps.ui.screens.map.serializeMapPointsJson
import com.scluzlep.newmangps.ui.screens.map.serializeSpeedsJson
import org.json.JSONArray
import org.json.JSONObject
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
    fun mapJsonSerializationUsesExpectedKeysAndFiltersInvalidCoords() {
        val wgsPoint = CoordinateTransform.LatLng(31.2304, 121.4737)
        val invalidNan = CoordinateTransform.LatLng(Double.NaN, 121.0)
        val invalidLat = CoordinateTransform.LatLng(89.0, 121.0)

        val pointsJson = JSONArray(
            serializeMapPointsJson(listOf(wgsPoint, invalidNan, invalidLat), MapSource.OSM)
        )
        assertEquals(1, pointsJson.length())
        val pt = pointsJson.getJSONObject(0)
        assertTrue(pt.has("lon"))
        assertTrue(pt.has("lat"))
        assertFalse(pt.has("longitude"))
        assertFalse(pt.has("latitude"))
        assertEquals(wgsPoint.longitude, pt.getDouble("lon"), 1e-9)
        assertEquals(wgsPoint.latitude, pt.getDouble("lat"), 1e-9)

        val amapPointsJson = JSONArray(
            serializeMapPointsJson(listOf(wgsPoint), MapSource.AMAP)
        )
        val amapPt = amapPointsJson.getJSONObject(0)
        val expectedGcj = MapSource.AMAP.toMapCoord(wgsPoint)
        assertEquals(expectedGcj.longitude, amapPt.getDouble("lon"), 1e-9)
        assertEquals(expectedGcj.latitude, amapPt.getDouble("lat"), 1e-9)

        val markersJson = JSONArray(
            serializeDeviceMarkersJson(
                listOf(
                    MapDeviceMarker(
                        teid = "861000000000001",
                        name = "Car 1",
                        longitude = wgsPoint.longitude,
                        latitude = wgsPoint.latitude,
                        direction = 90,
                        status = "moving",
                        iconUrl = "https://example.com/car.png"
                    ),
                    MapDeviceMarker(
                        teid = "bad",
                        name = "Invalid",
                        longitude = Double.POSITIVE_INFINITY,
                        latitude = 30.0,
                        direction = 0,
                        status = "offline"
                    )
                ),
                MapSource.TIANMAP
            )
        )
        assertEquals(1, markersJson.length())
        val markerObj = markersJson.getJSONObject(0)
        assertEquals("861000000000001", markerObj.getString("teid"))
        assertEquals("Car 1", markerObj.getString("name"))
        assertEquals(wgsPoint.longitude, markerObj.getDouble("lon"), 1e-9)
        assertEquals(wgsPoint.latitude, markerObj.getDouble("lat"), 1e-9)
        assertEquals(90, markerObj.getInt("direction"))
        assertEquals("moving", markerObj.getString("status"))
        assertEquals("https://example.com/car.png", markerObj.getString("iconUrl"))

        val fencesJson = JSONArray(
            serializeFencesJson(
                listOf(
                    MapFenceOverlay(
                        name = "Home",
                        isPolygon = false,
                        center = wgsPoint,
                        radiusMeters = 300
                    ),
                    MapFenceOverlay(
                        name = "Zone",
                        isPolygon = true,
                        vertices = listOf(
                            wgsPoint,
                            CoordinateTransform.LatLng(31.2314, 121.4747),
                            CoordinateTransform.LatLng(31.2294, 121.4757)
                        )
                    )
                ),
                MapSource.ARCGIS
            )
        )
        assertEquals(2, fencesJson.length())
        val circleFence = fencesJson.getJSONObject(0)
        assertEquals("Home", circleFence.getString("name"))
        assertFalse(circleFence.getBoolean("isPolygon"))
        assertEquals(300, circleFence.getInt("radiusMeters"))
        assertEquals(wgsPoint.longitude, circleFence.getJSONObject("center").getDouble("lon"), 1e-9)
        assertEquals(wgsPoint.latitude, circleFence.getJSONObject("center").getDouble("lat"), 1e-9)

        val polyFence = fencesJson.getJSONObject(1)
        assertEquals("Zone", polyFence.getString("name"))
        assertTrue(polyFence.getBoolean("isPolygon"))
        assertEquals(3, polyFence.getJSONArray("vertices").length())

        val speedsJson = JSONArray(serializeSpeedsJson(listOf(0, 45, 110)))
        assertEquals(3, speedsJson.length())
        assertEquals(45, speedsJson.getInt(1))

        val labelsJson = JSONObject(serializeLabelsJson(mapOf("start" to "起点", "end" to "终点")))
        assertEquals("起点", labelsJson.getString("start"))
        assertEquals("终点", labelsJson.getString("end"))
    }
}
