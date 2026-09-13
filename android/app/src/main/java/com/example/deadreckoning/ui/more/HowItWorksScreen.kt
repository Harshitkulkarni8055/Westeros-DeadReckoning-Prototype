package com.example.deadreckoning.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.deadreckoning.theme.DriftInkElevated
import com.example.deadreckoning.theme.DriftMint

/** Restyled after figmapics/Untitled10.png, with copy rewritten to
 * describe THIS prototype's actual pipeline (random-forest speed
 * predictor, nearest-segment map snap, fixed-alpha fusion) instead of the
 * full proposal's aspirational CNN-LSTM/HMM/learned-fusion architecture --
 * see FIGMA_REDESIGN_PLAN.md's decision. Stats are real numbers from
 * PLAN.md, not the mock's placeholder "<2MB" model-size figure (there is
 * no on-device model in this replay-only build).
 */
@Composable
fun HowItWorksScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxSize().background(DriftInk).safeDrawingPadding().padding(24.dp)) {
    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = "How it works", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close", tint = DriftMint) }
      }
      Spacer(Modifier.height(4.dp))
      Text(text = "This prototype's pipeline", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
      Spacer(Modifier.height(16.dp))
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DriftInkElevated),
        shape = RoundedCornerShape(20.dp),
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            PipelineIcon("IMU")
            PipelineIcon("Gyro+Mag")
            PipelineIcon("Speed RF")
          }
          Spacer(Modifier.height(16.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PipelineBox("Dead\nReckoning", Modifier.weight(1f))
            PipelineBox("Map Snap\n(nearest seg.)", Modifier.weight(1f))
            PipelineBox("Fixed-α\nFusion", Modifier.weight(1f))
          }
        }
      }
      Spacer(Modifier.height(20.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatChip("10 Hz", "Recording sample rate", Modifier.weight(1f))
        StatChip("~1.5 m/s", "Speed predictor MAE", Modifier.weight(1f))
      }
      Spacer(Modifier.height(24.dp))
      Text(
        text =
          "Random forest (not CNN-LSTM), nearest-segment snapping (not " +
            "HMM/Viterbi), and a fixed 0.9/0.1 GPS blend (not a learned " +
            "fusion network) -- the full proposal's targets, simplified for " +
            "this prototype. See PLAN.md for the reasoning.",
        color = DriftBodyText,
        fontSize = 13.sp,
        lineHeight = 19.sp,
      )
    }
    Spacer(Modifier.height(12.dp))
    OutlinedButton(
      onClick = onClose,
      modifier = Modifier.fillMaxWidth().height(52.dp),
      shape = RoundedCornerShape(50),
      colors = ButtonDefaults.outlinedButtonColors(contentColor = DriftMint),
    ) {
      Text("Back")
    }
  }
}

@Composable
private fun PipelineIcon(label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = "◆", color = DriftMint, fontSize = 20.sp)
    Spacer(Modifier.height(4.dp))
    Text(text = label, color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
  }
}

@Composable
private fun PipelineBox(label: String, modifier: Modifier = Modifier) {
  Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = DriftInk), shape = RoundedCornerShape(12.dp)) {
    Text(
      text = label,
      color = Color.White,
      fontWeight = FontWeight.Bold,
      fontSize = 12.sp,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp),
    )
  }
}

@Composable
private fun StatChip(value: String, label: String, modifier: Modifier = Modifier) {
  Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = DriftInkElevated), shape = RoundedCornerShape(16.dp)) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(value, color = DriftMint, fontWeight = FontWeight.Bold, fontSize = 18.sp)
      Text(label, color = DriftBodyText, fontSize = 12.sp)
    }
  }
}
