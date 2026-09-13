package com.example.deadreckoning.ui.hero

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deadreckoning.theme.DriftBodyText
import com.example.deadreckoning.theme.DriftInk
import com.example.deadreckoning.theme.DriftMint

/** Landing screen, restyled after the Figma "Drift Nav" hero
 * (figmapics/Untitled.png). The mock's "Start Navigation" button is
 * dropped -- there's no live navigation in this replay-only prototype,
 * see FIGMA_REDESIGN_PLAN.md's decision -- leaving "Explore Demo" as the
 * only entry point, into the replay list.
 */
@Composable
fun HeroScreen(onExploreDemo: () -> Unit, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxSize().background(DriftInk).safeDrawingPadding().padding(24.dp)) {
    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "⚡", color = DriftMint, fontSize = 20.sp)
        Spacer(Modifier.width(8.dp))
        Text(text = "DRIFT NAV", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
      }
      Spacer(Modifier.height(48.dp))
      Text(text = "Navigate", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp)
      Text(text = "beyond GPS.", color = DriftMint, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp)
      Spacer(Modifier.height(16.dp))
      Text(
        text =
          "A replay of the AI/ML-based Intelligent Dead Reckoning " +
            "prototype for GPS-denied navigation (SIH26168).",
        color = DriftBodyText,
        fontSize = 16.sp,
        lineHeight = 22.sp,
      )
      Spacer(Modifier.height(40.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        FlowStep(label = "Recorded\nSensors")
        Text("+", color = DriftMint, fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterVertically))
        FlowStep(label = "ML Speed\nModel")
        Text("→", color = DriftMint, fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterVertically))
        FlowStep(label = "Map + GNSS\nFusion")
      }
    }
    Spacer(Modifier.height(24.dp))
    OutlinedButton(
      onClick = onExploreDemo,
      modifier = Modifier.fillMaxWidth().height(56.dp),
      shape = RoundedCornerShape(50),
      colors = ButtonDefaults.outlinedButtonColors(contentColor = DriftMint),
      border = BorderStroke(1.dp, DriftMint),
    ) {
      Text("Explore Demo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
    Spacer(Modifier.height(24.dp))
    Text(
      text = "Smarter Navigation  ·  Safer Journeys",
      color = DriftBodyText,
      fontSize = 13.sp,
      modifier = Modifier.fillMaxWidth(),
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun FlowStep(label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = "•", color = DriftMint, fontSize = 22.sp)
    Spacer(Modifier.height(6.dp))
    Text(text = label, color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
  }
}
