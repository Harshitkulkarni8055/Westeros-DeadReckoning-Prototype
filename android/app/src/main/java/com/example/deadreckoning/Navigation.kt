package com.example.deadreckoning

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.deadreckoning.ui.common.MainTab
import com.example.deadreckoning.ui.hero.HeroScreen
import com.example.deadreckoning.ui.more.HowItWorksScreen
import com.example.deadreckoning.ui.more.MoreScreen
import com.example.deadreckoning.ui.playback.PlaybackScreen
import com.example.deadreckoning.ui.replaylist.ReplayListScreen
import com.example.deadreckoning.ui.system.SystemStatusScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Hero)

  fun switchTab(tab: MainTab) {
    backStack.clear()
    backStack.add(
      when (tab) {
        MainTab.REPLAY -> ReplayList
        MainTab.SYSTEM -> SystemStatus
        MainTab.MORE -> More
      },
    )
  }

  // The bottom bar's tab bar doesn't track a full per-tab back stack (a
  // prototype-scale simplification, see FIGMA_REDESIGN_PLAN.md): switching
  // tabs always jumps to that tab's root, and only the Replay tab keeps a
  // second entry (the open Playback screen) underneath it.
  val currentTab =
    when (backStack.lastOrNull()) {
      is SystemStatus -> MainTab.SYSTEM
      is More, is HowItWorks -> MainTab.MORE
      else -> MainTab.REPLAY
    }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Hero> { HeroScreen(onExploreDemo = { switchTab(MainTab.REPLAY) }) }
        entry<ReplayList> {
          ReplayListScreen(
            currentTab = currentTab,
            onTabSelected = ::switchTab,
            onSelect = { entry -> backStack.add(Playback(entry.assetFileName)) },
          )
        }
        entry<Playback> { key ->
          PlaybackScreen(
            assetFileName = key.assetFileName,
            currentTab = currentTab,
            onTabSelected = ::switchTab,
            onBack = { backStack.removeLastOrNull() },
          )
        }
        entry<SystemStatus> {
          SystemStatusScreen(
            currentTab = currentTab,
            onTabSelected = ::switchTab,
            onViewDetails = { backStack.add(HowItWorks) },
          )
        }
        entry<More> {
          MoreScreen(
            currentTab = currentTab,
            onTabSelected = ::switchTab,
            onHowItWorks = { backStack.add(HowItWorks) },
          )
        }
        entry<HowItWorks> { HowItWorksScreen(onClose = { backStack.removeLastOrNull() }) }
      },
  )
}
