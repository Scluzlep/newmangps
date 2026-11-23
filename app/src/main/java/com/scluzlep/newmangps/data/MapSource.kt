package com.scluzlep.newmangps.data

import com.scluzlep.newmangps.data.util.CoordinateTransform

enum class MapCoordSystem {
    GCJ02,
    WGS84
}

enum class MapSource(
    val id: String,
    val labelKey: String,
    val fallbackLabel: String,
    val coordSystem: MapCoordSystem,
    val supportsSatellite: Boolean,
    val supportsTraffic: Boolean,
    val maxZoom: Int
) {
    AMAP("amap", "GaodeMap", "高德地图", MapCoordSystem.GCJ02, true, true, 18),
    TENCENT("tencent", "TencentMap", "腾讯地图", MapCoordSystem.GCJ02, true, false, 18),
    TIANMAP("tianmap", "TianMap", "天地图", MapCoordSystem.WGS84, true, false, 18),
    BING("bing", "BingMap", "必应地图", MapCoordSystem.GCJ02, true, false, 18),
    ARCGIS("arcgis", "ArcGISMap", "ArcGIS 地图", MapCoordSystem.WGS84, true, false, 18),
    OSM("osm", "OpenStreetMap", "OpenStreetMap", MapCoordSystem.WGS84, false, false, 19),
    GOOGLE("google", "GoogleMap", "Google 地图", MapCoordSystem.GCJ02, true, false, 19);

    /** Converts canonical WGS-84 coordinates into this map source's tile coordinate space. */
    fun toMapCoord(wgs: CoordinateTransform.LatLng): CoordinateTransform.LatLng = when (coordSystem) {
        MapCoordSystem.GCJ02 -> CoordinateTransform.wgs84ToGcj02(wgs.latitude, wgs.longitude)
        MapCoordSystem.WGS84 -> wgs
    }

    /** Converts coordinates from this map source's tile space back into canonical WGS-84. */
    fun fromMapCoord(mapCoord: CoordinateTransform.LatLng): CoordinateTransform.LatLng = when (coordSystem) {
        MapCoordSystem.GCJ02 -> CoordinateTransform.gcj02ToWgs84(mapCoord.latitude, mapCoord.longitude)
        MapCoordSystem.WGS84 -> mapCoord
    }

    companion object {
        fun fromId(id: String?): MapSource = entries.firstOrNull { it.id == id } ?: AMAP
    }
}
