package com.example.deadreckoning.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One 100ms step of the replayed S1 recording, as exported by
 * `prototype/export_track.py`. Positions are lat/lon in degrees.
 */
@Serializable
data class TrackPoint(
  val t: Double,
  @SerialName("lat_gt") val latGt: Double,
  @SerialName("lon_gt") val lonGt: Double,
  @SerialName("lat_dr") val latDr: Double? = null,
  @SerialName("lon_dr") val lonDr: Double? = null,
  @SerialName("lat_mm") val latMm: Double,
  @SerialName("lon_mm") val lonMm: Double,
  @SerialName("lat_fused") val latFused: Double,
  @SerialName("lon_fused") val lonFused: Double,
  @SerialName("speed_pred") val speedPred: Double,
  @SerialName("heading_deg") val headingDeg: Double,
  @SerialName("gps_available") val gpsAvailable: Boolean,
  val alpha: Double,
  @SerialName("error_fused_m") val errorFusedM: Double,
)

@Serializable
data class TrackSummary(
  @SerialName("mean_error_fused_m") val meanErrorFusedM: Double,
  @SerialName("mean_error_fused_during_outage_m") val meanErrorFusedDuringOutageM: Double,
  @SerialName("max_error_fused_m") val maxErrorFusedM: Double,
)

@Serializable
data class TrackData(
  @SerialName("recording_id") val recordingId: String,
  val lat0: Double,
  val lon0: Double,
  @SerialName("outage_start_s") val outageStartS: Double,
  @SerialName("outage_duration_s") val outageDurationS: Double,
  val summary: TrackSummary,
  val rows: List<TrackPoint>,
)
