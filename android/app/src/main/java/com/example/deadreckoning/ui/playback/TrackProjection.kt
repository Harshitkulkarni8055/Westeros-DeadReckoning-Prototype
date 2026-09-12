package com.example.deadreckoning.ui.playback

import com.example.deadreckoning.data.TrackData
import kotlin.math.cos

/** Kotlin port of `prototype/geo.py`'s small-area equirectangular
 * projection: east/north metres relative to (lat0, lon0). Good enough for
 * plotting a single recording, not for large-area mapping.
 */
private const val EARTH_RADIUS_M = 6371000.0

data class LocalPoint(val x: Double, val y: Double)

fun latLonToLocalXy(lat: Double, lon: Double, lat0: Double, lon0: Double): LocalPoint {
  val lat0Rad = Math.toRadians(lat0)
  val lon0Rad = Math.toRadians(lon0)
  val x = (Math.toRadians(lon) - lon0Rad) * cos(lat0Rad) * EARTH_RADIUS_M
  val y = (Math.toRadians(lat) - lat0Rad) * EARTH_RADIUS_M
  return LocalPoint(x, y)
}

class ProjectedTrack(
  val groundTruth: List<LocalPoint>,
  val rawDeadReckoning: List<LocalPoint?>,
  val mapMatched: List<LocalPoint>,
  val fused: List<LocalPoint>,
  val minX: Double,
  val maxX: Double,
  val minY: Double,
  val maxY: Double,
)

fun projectTrack(track: TrackData): ProjectedTrack {
  val lat0 = track.lat0
  val lon0 = track.lon0

  val groundTruth = track.rows.map { latLonToLocalXy(it.latGt, it.lonGt, lat0, lon0) }
  val rawDeadReckoning =
    track.rows.map { row ->
      val lat = row.latDr
      val lon = row.lonDr
      if (lat != null && lon != null) latLonToLocalXy(lat, lon, lat0, lon0) else null
    }
  val mapMatched = track.rows.map { latLonToLocalXy(it.latMm, it.lonMm, lat0, lon0) }
  val fused = track.rows.map { latLonToLocalXy(it.latFused, it.lonFused, lat0, lon0) }

  val allPoints = groundTruth + rawDeadReckoning.filterNotNull() + mapMatched + fused
  return ProjectedTrack(
    groundTruth = groundTruth,
    rawDeadReckoning = rawDeadReckoning,
    mapMatched = mapMatched,
    fused = fused,
    minX = allPoints.minOf { it.x },
    maxX = allPoints.maxOf { it.x },
    minY = allPoints.minOf { it.y },
    maxY = allPoints.maxOf { it.y },
  )
}
