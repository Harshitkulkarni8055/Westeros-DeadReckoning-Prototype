package com.example.deadreckoning.ui.common

import androidx.compose.material.icons.Icons
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

/** The Figma design's bottom bar was Map / Replay / System / More; the Map
 * tab is dropped here since it needs live sensors + a real map SDK this
 * prototype doesn't have (see FIGMA_REDESIGN_PLAN.md's decisions).
 */
enum class MainTab(val label: String) {
  REPLAY("Replay"),
  SYSTEM("System"),
  MORE("More"),
}

@Composable
fun DriftBottomBar(current: MainTab, onSelect: (MainTab) -> Unit, modifier: Modifier = Modifier) {
  NavigationBar(modifier = modifier) {
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
