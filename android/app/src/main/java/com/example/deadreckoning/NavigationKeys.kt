package com.example.deadreckoning

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object ReplayList : NavKey

@Serializable data class Playback(val assetFileName: String) : NavKey
