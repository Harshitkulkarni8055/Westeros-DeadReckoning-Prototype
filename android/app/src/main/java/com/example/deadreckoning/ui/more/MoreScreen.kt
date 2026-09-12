package com.example.deadreckoning.ui.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deadreckoning.theme.DriftBodyText
import com.example.deadreckoning.theme.DriftInk
import com.example.deadreckoning.theme.DriftInkElevated
import com.example.deadreckoning.theme.DriftMint
import com.example.deadreckoning.ui.common.DriftBottomBar
import com.example.deadreckoning.ui.common.MainTab

/** New tab (no direct Figma equivalent besides housing the "How it works"
 * modal from figmapics/Untitled10.png) that gives the dropped Map tab's
 * bottom-bar slot a home for prototype-level info instead.
 */
@Composable
fun MoreScreen(
  currentTab: MainTab,
  onTabSelected: (MainTab) -> Unit,
  onHowItWorks: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    containerColor = DriftInk,
    bottomBar = { DriftBottomBar(current = currentTab, onSelect = onTabSelected) },
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
      Text(text = "More", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
      Spacer(Modifier.height(20.dp))
      Card(
        onClick = onHowItWorks,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DriftInkElevated),
        shape = RoundedCornerShape(16.dp),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("How it works", color = DriftMint, fontWeight = FontWeight.Bold)
          Text("The pipeline behind this replay, step by step.", color = DriftBodyText, fontSize = 13.sp)
        }
      }
      Spacer(Modifier.height(24.dp))
      Text(
        text =
          "Drift Nav is a prototype for SIH26168: AI/ML-based Intelligent " +
            "Dead Reckoning. It replays precomputed results from one Python " +
            "pipeline run on recorded IO-VNBD data -- there are no live " +
            "sensors or on-device models in this build.",
        color = DriftBodyText,
        fontSize = 13.sp,
        lineHeight = 19.sp,
      )
    }
  }
}
