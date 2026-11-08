package com.scluzlep.newmangps.data.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class RunSegment(
    val startSeconds: Long, val endSeconds: Long, val durationSeconds: Long,
    val distanceKm: Double, val averageSpeed: Double, val maximumSpeed: Double
)

data class ParkSegment(
    val startSeconds: Long, val endSeconds: Long, val durationSeconds: Long,
    val latitude: Double, val longitude: Double
)

fun runSegments(samples: List<ReportSample>, minimumSeconds: Long = 180): List<RunSegment> {
    val sorted = samples.filter { it.timeSeconds > 0 }.sortedBy(ReportSample::timeSeconds)
    if (sorted.size < 2) return emptyList()
    val ranges = mutableListOf<Pair<Int, Int>>()
    var start = -1
    var previousValid: ReportSample? = null
    sorted.forEachIndexed { index, sample ->
        if (!sample.isValidPosition) return@forEachIndexed
        if (sample.speed >= 2 && start < 0) start = index
        if (start >= 0 && (index == sorted.lastIndex ||
                (previousValid?.speed ?: 0.0) >= 2 && sample.isParked)) {
            ranges += start to index
            start = -1
        }
        previousValid = sample
    }
    if (start >= 0) {
        val lastValid = sorted.indices.lastOrNull { index -> sorted[index].isValidPosition }
        if (lastValid != null && lastValid > start) ranges += start to lastValid
    }
    val merged = mutableListOf<Pair<Int, Int>>()
    ranges.forEach { range ->
        if (merged.isNotEmpty() &&
            sorted[range.second].timeSeconds - sorted[range.first].timeSeconds < 60 &&
            sorted[range.first].timeSeconds - sorted[merged.last().second].timeSeconds < 60) {
            merged[merged.lastIndex] = merged.last().first to range.second
        } else merged += range
    }
    return merged.mapNotNull { (firstIndex, lastIndex) ->
        val first = sorted[firstIndex]
        val last = sorted[lastIndex]
        val duration = last.timeSeconds - first.timeSeconds
        val distance = last.mileageKm - first.mileageKm
        if (duration < minimumSeconds || (first.mileageKm > 0 && last.mileageKm > 0 && distance < 0.2)) {
            null
        } else {
            val speeds = sorted.subList(firstIndex, lastIndex + 1).map(ReportSample::speed)
            RunSegment(first.timeSeconds, last.timeSeconds, duration, distance.coerceAtLeast(0.0),
                speeds.average(), speeds.max())
        }
    }
}

fun parkSegments(samples: List<ReportSample>, minimumSeconds: Long = 600): List<ParkSegment> {
    val sorted = samples.asSequence().filter { it.timeSeconds > 0 && it.teState and 2048L == 0L }
        .filterNot { !it.isValidPosition && it.teState and 8192L != 0L &&
            it.teState and 4096L == 0L && it.teState and 268435456L == 0L }
        .sortedBy(ReportSample::timeSeconds).toList()
    if (sorted.size < 2) return emptyList()
    val candidates = mutableListOf<ParkSegment>()
    sorted.zipWithNext().forEachIndexed { index, (first, second) ->
        val seconds = second.timeSeconds - first.timeSeconds
        if (seconds <= 0) return@forEachIndexed
        val meters = distanceMeters(first, second)
        val qualifies = (first.isParked && second.isParked) ||
            (first.isParked && (meters < 100 || seconds > 600)) ||
            (index == sorted.size - 2 && second.isParked && (meters < 100 || seconds > 600)) ||
            (meters < 100 && seconds > 600) ||
            (seconds > 600 && meters / seconds < 0.1)
        if (qualifies) {
            val position = if (index == sorted.size - 2 && second.isParked) second else first
            candidates += ParkSegment(first.timeSeconds, second.timeSeconds, seconds,
                position.latitude, position.longitude)
        }
    }
    val merged = mutableListOf<ParkSegment>()
    candidates.forEach { candidate ->
        val last = merged.lastOrNull()
        if (last != null && candidate.startSeconds - last.endSeconds <= 600 &&
            distanceMeters(last.latitude, last.longitude, candidate.latitude, candidate.longitude) < 200) {
            merged[merged.lastIndex] = last.copy(
                endSeconds = candidate.endSeconds,
                durationSeconds = candidate.endSeconds - last.startSeconds
            )
        } else merged += candidate
    }
    return merged.filter { it.durationSeconds >= minimumSeconds }
}

private fun distanceMeters(a: ReportSample, b: ReportSample): Double =
    if (!a.isValidPosition || !b.isValidPosition) 0.0
    else distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)

private fun distanceMeters(latA: Double, lonA: Double, latB: Double, lonB: Double): Double {
    val lat1 = Math.toRadians(latA)
    val lat2 = Math.toRadians(latB)
    val dLat = lat2 - lat1
    val dLon = Math.toRadians(lonB - lonA)
    val h = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
    return 12_742_000.0 * asin(sqrt(h.coerceIn(0.0, 1.0)))
}
