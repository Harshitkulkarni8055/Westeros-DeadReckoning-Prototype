package com.example.deadreckoning.data

/** Hardcoded catalog of bundled replay recordings. Each entry points at one
 * asset JSON produced offline by `prototype/export_track.py` from a
 * different IO-VNBD recording -- there is no live pipeline or dynamic
 * discovery of routes in this prototype, per FRONTEND_PLAN.md's scope.
 *
 * `routeLabel` is cosmetic scenario dressing only (per user request: name
 * the demo routes after Indian cities/localities so the prototype "feels
 * Indian" for an SIH audience). The recorded IMU/GPS data underneath is
 * real IO-VNBD data captured in the UK (Coventry/Leicester area, see
 * PLAN.md) -- these labels don't change or misrepresent that data, the
 * road network drawn on the Map tab is still the genuine recorded route.
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
        routeLabel = "Hyderabad — HITEC City",
        conditionLabel = "GPS denied · 30 sec",
        assetFileName = "S1_track.json",
      ),
      ReplayEntry(
        index = "02",
        routeLabel = "Bengaluru — Indiranagar",
        conditionLabel = "GPS denied · 30 sec",
        assetFileName = "S3c_track.json",
      ),
      ReplayEntry(
        index = "03",
        routeLabel = "Mumbai — Bandra",
        conditionLabel = "GPS denied · 30 sec",
        assetFileName = "S3a_track.json",
      ),
    )
}
