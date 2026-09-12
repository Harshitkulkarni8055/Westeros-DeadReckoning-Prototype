package com.example.deadreckoning.ui.playback

import com.example.deadreckoning.data.TrackData

sealed interface PlaybackUiState {
  data object Loading : PlaybackUiState

  data class Error(val message: String) : PlaybackUiState

  data class Ready(
    val track: TrackData,
    val currentIndex: Int,
    val isPlaying: Boolean,
    val speedMultiplier: Int,
  ) : PlaybackUiState
}
