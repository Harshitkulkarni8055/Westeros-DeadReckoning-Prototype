package com.example.deadreckoning.ui.playback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deadreckoning.data.ActivePlaybackSnapshot
import com.example.deadreckoning.data.ActivePlaybackState
import com.example.deadreckoning.data.TrackRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Drives replay of one recording's precomputed track (see
 * FRONTEND_PLAN.md) -- there is no live sensor data, just a ticker
 * advancing through the rows loaded from [TrackRepository]. Also publishes
 * to [ActivePlaybackState] so the System Status tab can show real derived
 * status for whichever replay is open (see FIGMA_REDESIGN_PLAN.md).
 */
class PlaybackViewModel(private val repository: TrackRepository, private val routeLabel: String) : ViewModel() {
  private val _uiState = MutableStateFlow<PlaybackUiState>(PlaybackUiState.Loading)
  val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

  private var tickerJob: Job? = null

  init {
    viewModelScope.launch {
      _uiState.value =
        try {
          val track = repository.loadTrack()
          PlaybackUiState.Ready(track = track, currentIndex = 0, isPlaying = false, speedMultiplier = 1)
        } catch (e: Exception) {
          PlaybackUiState.Error(e.message ?: "Failed to load track")
        }
    }
    viewModelScope.launch {
      uiState.collect { state ->
        if (state is PlaybackUiState.Ready) {
          val row = state.track.rows[state.currentIndex]
          ActivePlaybackState.update(
            ActivePlaybackSnapshot(
              recordingId = state.track.recordingId,
              routeLabel = routeLabel,
              gpsAvailable = row.gpsAvailable,
            )
          )
        }
      }
    }
  }

  fun togglePlay() {
    val state = _uiState.value as? PlaybackUiState.Ready ?: return
    if (state.isPlaying) pause() else play()
  }

  private fun play() {
    val readyNow = _uiState.value as? PlaybackUiState.Ready ?: return
    _uiState.value =
      if (readyNow.currentIndex >= readyNow.track.rows.size - 1) {
        readyNow.copy(currentIndex = 0, isPlaying = true)
      } else {
        readyNow.copy(isPlaying = true)
      }

    tickerJob?.cancel()
    tickerJob =
      viewModelScope.launch {
        while (isActive) {
          val state = _uiState.value as? PlaybackUiState.Ready ?: break
          if (!state.isPlaying) break
          val nextIndex = state.currentIndex + 1
          if (nextIndex >= state.track.rows.size) {
            _uiState.value = state.copy(isPlaying = false)
            break
          }
          delay((100L / state.speedMultiplier).coerceAtLeast(1L))
          val latest = _uiState.value as? PlaybackUiState.Ready ?: break
          if (!latest.isPlaying) break
          _uiState.value = latest.copy(currentIndex = nextIndex)
        }
      }
  }

  private fun pause() {
    tickerJob?.cancel()
    val state = _uiState.value as? PlaybackUiState.Ready ?: return
    _uiState.value = state.copy(isPlaying = false)
  }

  fun seekTo(index: Int) {
    pause()
    val state = _uiState.value as? PlaybackUiState.Ready ?: return
    _uiState.value = state.copy(currentIndex = index.coerceIn(0, state.track.rows.size - 1))
  }

  fun restart() {
    seekTo(0)
    togglePlay()
  }

  fun cycleSpeed() {
    val state = _uiState.value as? PlaybackUiState.Ready ?: return
    val next = when (state.speedMultiplier) {
      1 -> 2
      2 -> 5
      else -> 1
    }
    _uiState.value = state.copy(speedMultiplier = next)
  }

  override fun onCleared() {
    tickerJob?.cancel()
    ActivePlaybackState.clear()
  }
}
