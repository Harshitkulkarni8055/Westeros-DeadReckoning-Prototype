package com.example.deadreckoning.ui.playback

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.deadreckoning.data.AssetTrackRepository
import com.example.deadreckoning.data.ReplayCatalog
import com.example.deadreckoning.data.TrackData
import com.example.deadreckoning.data.TrackPoint
import com.example.deadreckoning.theme.DriftBodyText
import com.example.deadreckoning.theme.DriftCardLight
import com.example.deadreckoning.theme.DriftMint
import com.example.deadreckoning.theme.DriftNavyText
import com.example.deadreckoning.theme.DriftSurfaceLight
import com.example.deadreckoning.ui.common.DriftBottomBar
import com.example.deadreckoning.ui.common.MainTab
import kotlin.math.min
import kotlin.random.Random

private object TrackColors {
  val groundTruth = Color(0xFF1565C0)
  val rawDeadReckoning = Color(0xFFEF6C00)
  val mapMatched = Color(0xFF8E24AA)
  val fused = Color(0xFF2E7D32)
}

// Distinct from every track color (blue/orange/green/purple) so the current
// position always stands out from the paths and the A/B markers alike.
private val VehicleColor = Color(0xFFFFC107)

// Neutral dark grey for the A/B start/end waypoint markers, deliberately
// distinct from every track color and the vehicle marker.
private val EndpointMarkerColor = Color(0xFF37474F)

@Composable
fun PlaybackScreen(
  assetFileName: String,
  currentTab: MainTab,
  onTabSelected: (MainTab) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val catalogEntry = remember(assetFileName) { ReplayCatalog.entries.find { it.assetFileName == assetFileName } }
  val routeLabel = catalogEntry?.routeLabel ?: assetFileName
  val viewModel: PlaybackViewModel =
    viewModel(key = assetFileName) {
      PlaybackViewModel(AssetTrackRepository(context.applicationContext, assetFileName), routeLabel)
    }
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  Scaffold(
    modifier = modifier,
    containerColor = DriftSurfaceLight,
    bottomBar = { DriftBottomBar(current = currentTab, onSelect = onTabSelected) },
  ) { padding ->
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
      when (val state = uiState) {
        is PlaybackUiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
        is PlaybackUiState.Error -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Failed to load track: ${state.message}")
          }
        }
        is PlaybackUiState.Ready -> {
          PlaybackContent(
            state = state,
            catalogIndex = catalogEntry?.index ?: "--",
            routeLabel = routeLabel,
            onTogglePlay = viewModel::togglePlay,
            onSeek = viewModel::seekTo,
            onCycleSpeed = viewModel::cycleSpeed,
            onRestart = viewModel::restart,
            onBack = onBack,
          )
        }
      }
    }
  }
}

@Composable
private fun PlaybackContent(
  state: PlaybackUiState.Ready,
  catalogIndex: String,
  routeLabel: String,
  onTogglePlay: () -> Unit,
  onSeek: (Int) -> Unit,
  onCycleSpeed: () -> Unit,
  onRestart: () -> Unit,
  onBack: () -> Unit,
) {
  val currentPoint = state.track.rows[state.currentIndex]
  Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      TextButton(onClick = onBack) { Text("‹", fontSize = 22.sp, color = DriftNavyText) }
      Spacer(Modifier.width(4.dp))
      Text(
        text = "Scenario $catalogIndex · $routeLabel",
        color = DriftNavyText,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
      )
    }
    Spacer(Modifier.height(12.dp))
    TrackLegend()
    Spacer(Modifier.height(12.dp))
    Card(
      modifier = Modifier.fillMaxWidth().weight(1f),
      colors = CardDefaults.cardColors(containerColor = DriftCardLight),
      shape = RoundedCornerShape(20.dp),
    ) {
      TrackCanvas(track = state.track, currentIndex = state.currentIndex, modifier = Modifier.fillMaxSize().padding(8.dp))
    }
    Spacer(Modifier.height(12.dp))
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = DriftCardLight),
      shape = RoundedCornerShape(20.dp),
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("Position Estimation", color = DriftNavyText, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        StatusRow(point = currentPoint)
        Spacer(Modifier.height(12.dp))
        PlaybackControls(
          currentIndex = state.currentIndex,
          totalPoints = state.track.rows.size,
          isPlaying = state.isPlaying,
          onTogglePlay = onTogglePlay,
          onSeek = onSeek,
        )
      }
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
      AssistChip(
        onClick = onRestart,
        leadingIcon = { Text("↻", color = DriftNavyText) },
        label = { Text("Replay") },
        colors =
          AssistChipDefaults.assistChipColors(
            containerColor = DriftMint.copy(alpha = 0.15f),
            labelColor = DriftNavyText,
            leadingIconContentColor = DriftNavyText,
          ),
      )
      TextButton(onClick = onCycleSpeed) { Text("${state.speedMultiplier}x", color = DriftNavyText, fontWeight = FontWeight.Bold) }
    }
  }
}

@Composable
private fun TrackCanvas(track: TrackData, currentIndex: Int, modifier: Modifier = Modifier) {
  val projected = remember(track) { projectTrack(track) }

  Canvas(modifier = modifier) {
    val worldWidth = (projected.maxX - projected.minX).coerceAtLeast(1.0)
    val worldHeight = (projected.maxY - projected.minY).coerceAtLeast(1.0)
    val paddingPx = 32f
    val scale =
      min(
        (size.width - 2 * paddingPx) / worldWidth.toFloat(),
        (size.height - 2 * paddingPx) / worldHeight.toFloat(),
      ).coerceAtLeast(0.01f)
    val centerWorldX = (projected.minX + projected.maxX) / 2.0
    val centerWorldY = (projected.minY + projected.maxY) / 2.0
    val centerScreenX = size.width / 2f
    val centerScreenY = size.height / 2f

    fun toOffset(p: LocalPoint): Offset {
      val sx = centerScreenX + ((p.x - centerWorldX) * scale).toFloat()
      val sy = centerScreenY - ((p.y - centerWorldY) * scale).toFloat()
      return Offset(sx, sy)
    }

    // Purely decorative backdrop -- a dummy street grid so the replay reads
    // as "a vehicle on a map" rather than three lines floating in a void.
    // Not derived from real road data (that's map_matcher.py's OSM graph,
    // which this offline replay doesn't bundle).
    drawDummyStreetGrid()

    // Map-matched isn't drawn: per fusion.py it's pixel-identical to the
    // fused track during the entire GPS outage, and within ~5m of it
    // otherwise -- it would only ever be drawn invisibly under "fused".
    drawProjectedPath(projected.groundTruth, ::toOffset, TrackColors.groundTruth, 6f)
    drawProjectedPath(projected.rawDeadReckoning, ::toOffset, TrackColors.rawDeadReckoning, 3f)
    drawProjectedPath(projected.fused, ::toOffset, TrackColors.fused, 5f)

    // Endpoint markers so the direction of travel is unambiguous: "A" is
    // where the recording starts, "B" is where it ends. Anchored to the
    // fused track -- the app's final output -- rather than ground truth.
    drawEndpointMarker(toOffset(projected.fused.first()), "A")
    drawEndpointMarker(toOffset(projected.fused.last()), "B")

    val trailLength = 30
    val trailStart = (currentIndex - trailLength).coerceAtLeast(0)
    for (i in trailStart..currentIndex) {
      val fraction =
        if (currentIndex == trailStart) 1f else (i - trailStart).toFloat() / (currentIndex - trailStart).toFloat()
      drawCircle(
        color = VehicleColor.copy(alpha = 0.15f + 0.6f * fraction),
        radius = 5f,
        center = toOffset(projected.fused[i]),
      )
    }
    val vehicleOffset = toOffset(projected.fused[currentIndex])
    drawCircle(color = VehicleColor, radius = 15f, center = vehicleOffset)
    drawCircle(color = Color.White, radius = 15f, center = vehicleOffset, style = Stroke(width = 3f))
  }
}

private fun DrawScope.drawDummyStreetGrid() {
  // Pale base + irregular blocks + a couple of thicker "arterial" roads and
  // a few soft park patches -- purely decorative texture so the replay
  // reads as "a vehicle on a map," not real road geometry (that's
  // map_matcher.py's OSM graph, which this offline replay doesn't bundle).
  drawRect(color = Color(0xFFF3F1F8))

  val rng = Random(1)
  val minorColor = Color(0xFFD6D0E0)
  val majorColor = Color(0xFFE3C583)
  val parkColor = Color(0xFFCFE3CB).copy(alpha = 0.7f)

  var x = 0f
  while (x < size.width) {
    x += size.width / 7f * (0.6f + rng.nextFloat())
    if (x < size.width) drawLine(minorColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2f)
  }
  var y = 0f
  while (y < size.height) {
    y += size.height / 9f * (0.6f + rng.nextFloat())
    if (y < size.height) drawLine(minorColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
  }

  drawLine(majorColor, Offset(0f, size.height * 0.32f), Offset(size.width, size.height * 0.42f), strokeWidth = 6f)
  drawLine(majorColor, Offset(size.width * 0.62f, 0f), Offset(size.width * 0.48f, size.height), strokeWidth = 6f)

  drawRoundRect(
    color = parkColor,
    topLeft = Offset(size.width * 0.08f, size.height * 0.62f),
    size = Size(size.width * 0.2f, size.height * 0.16f),
    cornerRadius = CornerRadius(14f, 14f),
  )
  drawRoundRect(
    color = parkColor,
    topLeft = Offset(size.width * 0.72f, size.height * 0.12f),
    size = Size(size.width * 0.16f, size.height * 0.14f),
    cornerRadius = CornerRadius(14f, 14f),
  )
}

private fun DrawScope.drawEndpointMarker(center: Offset, label: String) {
  drawCircle(color = Color.White, radius = 24f, center = center)
  drawCircle(color = EndpointMarkerColor, radius = 24f, center = center, style = Stroke(width = 4f))
  drawContext.canvas.nativeCanvas.drawText(
    label,
    center.x,
    center.y + 10f,
    android.graphics.Paint().apply {
      color = EndpointMarkerColor.toArgb()
      textAlign = android.graphics.Paint.Align.CENTER
      textSize = 32f
      isFakeBoldText = true
      isAntiAlias = true
    },
  )
}

private fun DrawScope.drawProjectedPath(
  points: List<LocalPoint?>,
  toOffset: (LocalPoint) -> Offset,
  color: Color,
  strokeWidthPx: Float,
) {
  val path = Path()
  var started = false
  for (p in points) {
    if (p == null) {
      started = false
      continue
    }
    val offset = toOffset(p)
    if (!started) {
      path.moveTo(offset.x, offset.y)
      started = true
    } else {
      path.lineTo(offset.x, offset.y)
    }
  }
  drawPath(path, color = color, style = Stroke(width = strokeWidthPx))
}

@Composable
private fun TrackLegend(modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    LegendItem(TrackColors.groundTruth, "Ground Truth")
    LegendItem(TrackColors.rawDeadReckoning, "Raw DR")
    LegendItem(TrackColors.fused, "AI Fusion")
    LegendItem(VehicleColor, "Current Position")
    LegendItem(EndpointMarkerColor, "Start/End (A/B)")
  }
}

@Composable
private fun LegendItem(color: Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    Box(modifier = Modifier.size(10.dp).background(color = color, shape = CircleShape))
    Text(text = label, color = DriftBodyText, style = MaterialTheme.typography.labelSmall)
  }
}

@Composable
private fun StatusRow(point: TrackPoint, modifier: Modifier = Modifier) {
  Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    StatusStat(label = "Confidence", value = "${(point.alpha * 100).toInt()}%")
    StatusStat(label = "Speed", value = "%.1f m/s".format(point.speedPred))
    StatusStat(label = "Heading", value = "%.0f°".format(point.headingDeg))
  }
}

@Composable
private fun StatusStat(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = value, color = DriftNavyText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(text = label, color = DriftBodyText, style = MaterialTheme.typography.labelSmall)
  }
}

@Composable
private fun PlaybackControls(
  currentIndex: Int,
  totalPoints: Int,
  isPlaying: Boolean,
  onTogglePlay: () -> Unit,
  onSeek: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Slider(
      value = currentIndex.toFloat(),
      onValueChange = { onSeek(it.toInt()) },
      valueRange = 0f..(totalPoints - 1).toFloat().coerceAtLeast(0f),
      modifier = Modifier.weight(1f),
      colors = SliderDefaults.colors(thumbColor = DriftMint, activeTrackColor = DriftMint),
    )
    Spacer(Modifier.width(8.dp))
    FilledIconButton(onClick = onTogglePlay, colors = IconButtonDefaults.filledIconButtonColors(containerColor = DriftMint)) {
      if (isPlaying) {
        Text("❚❚", color = Color.White, fontSize = 14.sp)
      } else {
        Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White)
      }
    }
  }
}
