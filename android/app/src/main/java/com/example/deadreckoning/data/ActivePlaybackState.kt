package com.example.deadreckoning.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A snapshot of whichever replay is currently open in [com.example.
 * deadreckoning.ui.playback.PlaybackScreen], published on every tick so
 * [com.example.deadreckoning.ui.system.SystemStatusScreen] can show real
 * derived status (e.g. "GNSS unavailable") instead of a fabricated one --
 * see FIGMA_REDESIGN_PLAN.md's System Status decision. Cleared when the
 * playback screen is left, so System Status goes back to an explicit idle
 * state rather than showing stale data.
 */
data class ActivePlaybackSnapshot(
  val recordingId: String,
  val routeLabel: String,
  val gpsAvailable: Boolean,
)

object ActivePlaybackState {
  private val _current = MutableStateFlow<ActivePlaybackSnapshot?>(null)
  val current: StateFlow<ActivePlaybackSnapshot?> = _current

  fun update(snapshot: ActivePlaybackSnapshot) {
    _current.value = snapshot
  }

  fun clear() {
    _current.value = null
  }
}
