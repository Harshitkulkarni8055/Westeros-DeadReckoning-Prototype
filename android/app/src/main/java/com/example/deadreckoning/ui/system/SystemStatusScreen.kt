package com.example.deadreckoning.ui.system

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deadreckoning.data.ActivePlaybackSnapshot
import com.example.deadreckoning.data.ActivePlaybackState
import com.example.deadreckoning.theme.DriftBodyText
import com.example.deadreckoning.theme.DriftInk
import com.example.deadreckoning.theme.DriftInkElevated
import com.example.deadreckoning.theme.DriftMint
import com.example.deadreckoning.theme.DriftStatusAmber
import com.example.deadreckoning.theme.DriftStatusGreen
import com.example.deadreckoning.theme.DriftStatusRed
import com.example.deadreckoning.ui.common.DriftBottomBar
import com.example.deadreckoning.ui.common.MainTab

private data class StatusRow(val label: String, val detail: String, val color: Color)

/** Restyled after figmapics/Untitled7.png, but every row is derived from
 * [ActivePlaybackState] -- whichever replay is actually open in
 * PlaybackScreen -- rather than the mock's hardcoded "Active" rows (see
 * FIGMA_REDESIGN_PLAN.md's System Status decision). With nothing playing,
 * this shows an explicit idle state instead of fabricated status.
 */
@Composable
fun SystemStatusScreen(
  currentTab: MainTab,
  onTabSelected: (MainTab) -> Unit,
  onViewDetails: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val snapshot by ActivePlaybackState.current.collectAsState()

  Scaffold(
    modifier = modifier,
    containerColor = DriftInk,
    bottomBar = { DriftBottomBar(current = currentTab, onSelect = onTabSelected) },
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
      Text(
        text = "System Status",
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
      )
      Spacer(Modifier.height(16.dp))
      HeadlinePill(snapshot)
      Spacer(Modifier.height(20.dp))
      statusRows(snapshot).forEach { row ->
        StatusRowCard(row)
        Spacer(Modifier.height(12.dp))
      }
      Spacer(Modifier.height(12.dp))
      OutlinedButton(
        onClick = onViewDetails,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = DriftMint),
      ) {
        Text("View system details  →")
      }
    }
  }
}

@Composable
private fun HeadlinePill(snapshot: ActivePlaybackSnapshot?) {
  val (text, color) =
    when {
      snapshot == null -> "No replay playing" to DriftBodyText
      !snapshot.gpsAvailable -> "Dead Reckoning Active (${snapshot.routeLabel})" to DriftStatusAmber
      else -> "AI Fusion Active (${snapshot.routeLabel})" to DriftMint
    }
  Row(
    modifier =
      Modifier.clip(RoundedCornerShape(50))
        .background(color.copy(alpha = 0.15f))
        .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Dot(color)
    Spacer(Modifier.width(8.dp))
    Text(text, color = color, fontWeight = FontWeight.Bold)
  }
}

private fun statusRows(snapshot: ActivePlaybackSnapshot?): List<StatusRow> {
  if (snapshot == null) {
    return listOf(
      StatusRow("GNSS", "Idle", DriftBodyText),
      StatusRow("IMU", "Idle", DriftBodyText),
      StatusRow("Map Matching", "Idle", DriftBodyText),
      StatusRow("Fusion Pipeline", "Idle", DriftBodyText),
    )
  }
  return listOf(
    StatusRow("GNSS", if (snapshot.gpsAvailable) "Active" else "Unavailable", if (snapshot.gpsAvailable) DriftStatusGreen else DriftStatusRed),
    StatusRow("IMU", "Active", DriftStatusGreen),
    // map_matcher.py only snaps when GPS is unavailable -- see fusion.py's
    // run_fused_pipeline -- so this row reflects that, not a fake always-on.
    StatusRow("Map Matching", if (snapshot.gpsAvailable) "Standby" else "Active", if (snapshot.gpsAvailable) DriftBodyText else DriftStatusGreen),
    StatusRow("Fusion Pipeline", "Active", DriftStatusGreen),
  )
}

@Composable
private fun StatusRowCard(row: StatusRow) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = DriftInkElevated),
    shape = RoundedCornerShape(16.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Dot(row.color)
      Spacer(Modifier.width(12.dp))
      Text(row.label, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
      Text(row.detail, color = row.color)
    }
  }
}

@Composable
private fun Dot(color: Color) {
  Row(modifier = Modifier.size(10.dp).clip(CircleShape).background(color)) {}
}
