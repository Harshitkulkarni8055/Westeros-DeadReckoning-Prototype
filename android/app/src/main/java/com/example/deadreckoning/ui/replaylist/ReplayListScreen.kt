package com.example.deadreckoning.ui.replaylist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deadreckoning.data.ReplayCatalog
import com.example.deadreckoning.data.ReplayEntry
import com.example.deadreckoning.theme.DriftBodyText
import com.example.deadreckoning.theme.DriftCardLight
import com.example.deadreckoning.theme.DriftNavyText
import com.example.deadreckoning.theme.DriftSurfaceLight
import com.example.deadreckoning.ui.common.DriftBottomBar
import com.example.deadreckoning.ui.common.MainTab

/** "Test Scenarios" screen, restyled after figmapics/Untitled8.png --
 * functionally the same replay picker as before, now styled to match.
 */
@Composable
fun ReplayListScreen(
  currentTab: MainTab,
  onTabSelected: (MainTab) -> Unit,
  onSelect: (ReplayEntry) -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    containerColor = DriftSurfaceLight,
    bottomBar = { DriftBottomBar(current = currentTab, onSelect = onTabSelected) },
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
      Text(text = "Test Scenarios", color = DriftNavyText, fontWeight = FontWeight.Bold, fontSize = 26.sp)
      Spacer(Modifier.height(16.dp))
      LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(ReplayCatalog.entries) { entry -> ReplayCard(entry = entry, onClick = { onSelect(entry) }) }
      }
    }
  }
}

@Composable
private fun ReplayCard(entry: ReplayEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Card(
    onClick = onClick,
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = DriftCardLight),
    shape = RoundedCornerShape(20.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier.size(48.dp).background(DriftNavyText, RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Text(entry.index, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(entry.routeLabel, color = DriftNavyText, fontWeight = FontWeight.Bold, fontSize = 17.sp)
          Text(entry.conditionLabel, color = DriftBodyText, fontSize = 13.sp)
        }
      }
      Spacer(Modifier.height(12.dp))
      TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(containerColor = DriftNavyText, contentColor = Color.White),
        shape = RoundedCornerShape(50),
      ) {
        Text("Replay  →", fontWeight = FontWeight.Bold)
      }
    }
  }
}
