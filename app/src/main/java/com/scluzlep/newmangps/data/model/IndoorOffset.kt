package com.scluzlep.newmangps.data.model

data class IndoorOffset(
    val teid: String, val name: String, val address: String, val enabled: Boolean,
    val longitude: Double, val latitude: Double, val radiusMeters: Int,
    val targetLongitude: Double, val targetLatitude: Double
) {
    companion object {
        fun fromRow(row: Map<String, String>) = IndoorOffset(
            row["strTEID"].orEmpty(), row["strName"].orEmpty(), row["strAddress"].orEmpty(),
            row["isEnable"]?.toIntOrNull() == 1,
            row["dbLon"]?.toDoubleOrNull() ?: 0.0, row["dbLat"]?.toDoubleOrNull() ?: 0.0,
            row["nRadius"]?.toIntOrNull() ?: 0,
            row["dbLonOffset"]?.toDoubleOrNull() ?: 0.0,
            row["dbLatOffset"]?.toDoubleOrNull() ?: 0.0
        )
    }
}
