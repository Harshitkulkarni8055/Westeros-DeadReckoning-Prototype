package com.example.deadreckoning.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.deadreckoning.theme.DriftMint
import com.example.deadreckoning.theme.DriftNavyText

/** Matches the Figma design's Map / Replay / System / More bottom bar --
 * unlike the first redesign pass, Map is now real (see ui/map/MapScreen.kt
 * and FIGMA_REDESIGN_PLAN.md's revised decision).
 */
enum class MainTab(val label: String) {
  MAP("Map"),
  REPLAY("Replay"),
  SYSTEM("System"),
  MORE("More"),
}

@Composable
fun DriftBottomBar(current: MainTab, onSelect: (MainTab) -> Unit, modifier: Modifier = Modifier) {
  NavigationBar(modifier = modifier) {
    NavigationBarItem(
      selected = current == MainTab.MAP,
      onClick = { onSelect(MainTab.MAP) },
      icon = { Icon(Icons.Filled.Place, contentDescription = null) },
      label = { Text(MainTab.MAP.label) },
      colors = tabColors(),
    )
    NavigationBarItem(
      selected = current == MainTab.REPLAY,
      onClick = { onSelect(MainTab.REPLAY) },
      icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
      label = { Text(MainTab.REPLAY.label) },
      colors = tabColors(),
    )
    NavigationBarItem(
      selected = current == MainTab.SYSTEM,
      onClick = { onSelect(MainTab.SYSTEM) },
      icon = { Icon(Icons.Filled.Star, contentDescription = null) },
      label = { Text(MainTab.SYSTEM.label) },
      colors = tabColors(),
    )
    NavigationBarItem(
      selected = current == MainTab.MORE,
      onClick = { onSelect(MainTab.MORE) },
      icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
      label = { Text(MainTab.MORE.label) },
      colors = tabColors(),
    )
  }
}

@Composable
private fun tabColors() =
  NavigationBarItemDefaults.colors(
    selectedIconColor = DriftNavyText,
    selectedTextColor = DriftNavyText,
    indicatorColor = DriftMint.copy(alpha = 0.25f),
  )
