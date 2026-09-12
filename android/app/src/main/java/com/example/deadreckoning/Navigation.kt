package com.example.deadreckoning

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.deadreckoning.ui.playback.PlaybackScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Playback)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Playback> {
          PlaybackScreen(modifier = Modifier.safeDrawingPadding().padding(16.dp))
        }
      },
  )
}
