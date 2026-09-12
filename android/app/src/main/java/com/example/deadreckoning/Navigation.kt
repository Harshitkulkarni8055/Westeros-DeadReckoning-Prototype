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
import com.example.deadreckoning.ui.replaylist.ReplayListScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(ReplayList)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<ReplayList> {
          ReplayListScreen(
            onSelect = { entry -> backStack.add(Playback(entry.assetFileName)) },
            modifier = Modifier.safeDrawingPadding().padding(16.dp),
          )
        }
        entry<Playback> { key ->
          PlaybackScreen(
            assetFileName = key.assetFileName,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding().padding(16.dp),
          )
        }
      },
  )
}
