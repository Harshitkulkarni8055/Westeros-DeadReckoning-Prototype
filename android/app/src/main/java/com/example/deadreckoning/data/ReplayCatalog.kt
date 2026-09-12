package com.example.deadreckoning.data

/** Hardcoded catalog of bundled replay recordings. Each entry points at one
 * asset JSON produced offline by `prototype/export_track.py` from a
 * different IO-VNBD recording -- there is no live pipeline or dynamic
 * discovery of routes in this prototype, per FRONTEND_PLAN.md's scope.
 */
data class ReplayEntry(
  val title: String,
  val routeLabel: String,
  val assetFileName: String,
)

object ReplayCatalog {
  val entries =
    listOf(
      ReplayEntry(title = "Replay 1", routeLabel = "Route A → B", assetFileName = "S1_track.json"),
      ReplayEntry(title = "Replay 2", routeLabel = "Route C → D", assetFileName = "S3c_track.json"),
      ReplayEntry(title = "Replay 3", routeLabel = "Route E → F", assetFileName = "S3a_track.json"),
    )
}
