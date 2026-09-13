package com.example.deadreckoning.data

/** Hardcoded catalog of bundled replay recordings. Each entry points at one
 * asset JSON produced offline by `prototype/export_track.py` from a
 * different IO-VNBD recording -- there is no live pipeline or dynamic
 * discovery of routes in this prototype, per FRONTEND_PLAN.md's scope.
 *
 * `conditionLabel` states the real simulated-outage duration each
 * recording was exported with (all three currently use a 30s outage --
 * see FRONTEND_PLAN.md's F4 milestone) rather than inventing per-scenario
 * flavor text.
 */
data class ReplayEntry(
  val index: String,
  val routeLabel: String,
  val conditionLabel: String,
  val assetFileName: String,
)

object ReplayCatalog {
  val entries =
    listOf(
      ReplayEntry(
        index = "01",
        routeLabel = "Route A-B",
        conditionLabel = "GPS denied · 30 sec",
        assetFileName = "S1_track.json",
      ),
      ReplayEntry(
        index = "02",
        routeLabel = "Route C-D",
        conditionLabel = "GPS denied · 30 sec",
        assetFileName = "S3c_track.json",
      ),
      ReplayEntry(
        index = "03",
        routeLabel = "Route E-F",
        conditionLabel = "GPS denied · 30 sec",
        assetFileName = "S3a_track.json",
      ),
    )
}
