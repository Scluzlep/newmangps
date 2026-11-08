package com.scluzlep.newmangps.data.model

private data class ScalePoint(val percent: Double, val value: Double)

private fun parseScale(value: String?): List<ScalePoint> = value.orEmpty().split(';').mapNotNull { entry ->
    val parts = entry.split(',')
    if (parts.size != 2) null else {
        val percent = parts[0].trim().toDoubleOrNull()
        val raw = parts[1].trim().toDoubleOrNull()
        if (percent == null || raw == null) null else ScalePoint(percent, raw)
    }
}.sortedBy(ScalePoint::value)

private fun correct(raw: Double, scale: List<ScalePoint>): Double {
    val pair = scale.zipWithNext().firstOrNull { (first, second) ->
        raw >= first.value && raw <= second.value && first.value != second.value
    } ?: return raw
    val (first, second) = pair
    return (second.percent * (raw - first.value) + first.percent * (second.value - raw)) /
        (second.value - first.value)
}

fun fuelReading(sample: ReportSample, detail: DeviceDetail?): Pair<Double, String>? {
    val raw = sample.fuelRaw ?: return null
    val boxSize = detail?.rawFields?.get("nFuelBoxSize")?.toDoubleOrNull() ?: 0.0
    val scale = parseScale(detail?.rawFields?.get("strFuelScale"))
    return if (boxSize > 0 && scale.size >= 2) {
        (correct(raw, scale).toInt() * boxSize / 100.0) to "升"
    } else raw to "原始读数"
}

fun temperatureReading(sample: ReportSample, detail: DeviceDetail?, channel: Int): Double? {
    if (channel == 0) {
        val raw = sample.temperatureC ?: return null
        if (kotlin.math.abs(raw) > 200) return null
        val scale = parseScale(detail?.rawFields?.get("strTempScale"))
        return if (scale.size >= 2) correct(raw, scale) else raw
    }
    if (channel !in 1..4) return null
    val value = sample.other.split(';').firstNotNullOfOrNull { part ->
        val fields = part.split('=', limit = 2)
        if (fields.size == 2 && fields[0].trim().equals("TEMP$channel", true)) {
            fields[1].trim().toDoubleOrNull()
        } else null
    } ?: return null
    return value.takeIf { kotlin.math.abs(it) < 127 }?.div(10)
}
