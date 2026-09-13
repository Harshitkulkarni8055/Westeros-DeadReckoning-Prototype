package com.example.deadreckoning

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Hero : NavKey

@Serializable data object DeviceCheck : NavKey

@Serializable data object AllSet : NavKey

@Serializable data object MapHome : NavKey

@Serializable data object ReplayList : NavKey

@Serializable data class Playback(val assetFileName: String) : NavKey

@Serializable data object SystemStatus : NavKey

@Serializable data object More : NavKey

@Serializable data object HowItWorks : NavKey
