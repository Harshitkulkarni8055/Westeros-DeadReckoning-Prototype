package com.example.deadreckoning.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deadreckoning.theme.DriftBodyText
import com.example.deadreckoning.theme.DriftInk
import com.example.deadreckoning.theme.DriftMint

/** Restyled after figmapics/Untitled4.png. */
@Composable
fun AllSetScreen(onEnterApp: () -> Unit, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxSize().background(DriftInk).safeDrawingPadding().padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("DRIFT NAV", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.weight(1f))
    Box(
      modifier = Modifier.size(140.dp).clip(CircleShape).background(DriftMint.copy(alpha = 0.12f)),
      contentAlignment = Alignment.Center,
    ) {
      Text("⚡", color = DriftMint, fontSize = 40.sp)
    }
    Spacer(Modifier.height(24.dp))
    Text("You're all set!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
    Spacer(Modifier.height(8.dp))
    Text(
      "This replay-only prototype is ready.",
      color = DriftBodyText,
      fontSize = 15.sp,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.weight(1f))
    Button(
      onClick = onEnterApp,
      modifier = Modifier.fillMaxWidth().height(56.dp),
      shape = RoundedCornerShape(50),
      colors = ButtonDefaults.buttonColors(containerColor = DriftMint, contentColor = DriftInk),
    ) {
      Text("Enter App", fontWeight = FontWeight.Bold)
    }
  }
}
