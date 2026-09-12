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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.deadreckoning.data.AssetTrackRepository
import com.example.deadreckoning.data.TrackData
import com.example.deadreckoning.data.TrackPoint
import kotlin.math.min

private object TrackColors {
  val groundTruth = Color(0xFF1565C0)
  val rawDeadReckoning = Color(0xFFEF6C00)
  val mapMatched = Color(0xFF8E24AA)
  val fused = Color(0xFF2E7D32)
}

@Composable
fun PlaybackScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val viewModel: PlaybackViewModel =
    viewModel { PlaybackViewModel(AssetTrackRepository(context.applicationContext)) }
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  when (val state = uiState) {
    is PlaybackUiState.Loading -> {
      Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    }
    is PlaybackUiState.Error -> {
      Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Failed to load track: ${state.message}")
      }
    }
    is PlaybackUiState.Ready -> {
      PlaybackContent(
        state = state,
        onTogglePlay = viewModel::togglePlay,
        onSeek = viewModel::seekTo,
        onCycleSpeed = viewModel::cycleSpeed,
        modifier = modifier,
      )
    }
  }
}

@Composable
private fun PlaybackContent(
  state: PlaybackUiState.Ready,
  onTogglePlay: () -> Unit,
  onSeek: (Int) -> Unit,
  onCycleSpeed: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val currentPoint = state.track.rows[state.currentIndex]
  Column(modifier = modifier.fillMaxSize()) {
    Text(
      text = "IO-VNBD ${state.track.recordingId} — GPS-denied navigation replay",
      style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.height(8.dp))
    TrackCanvas(
      track = state.track,
      currentIndex = state.currentIndex,
      modifier = Modifier.fillMaxWidth().weight(1f),
    )
    Spacer(Modifier.height(8.dp))
    TrackLegend()
    Spacer(Modifier.height(8.dp))
    StatusRow(point = currentPoint)
    Spacer(Modifier.height(8.dp))
    PlaybackControls(
      currentIndex = state.currentIndex,
      totalPoints = state.track.rows.size,
      isPlaying = state.isPlaying,
      speedMultiplier = state.speedMultiplier,
      onTogglePlay = onTogglePlay,
      onSeek = onSeek,
      onCycleSpeed = onCycleSpeed,
    )
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

    // Map-matched isn't drawn: per fusion.py it's pixel-identical to the
    // fused track during the entire GPS outage, and within ~5m of it
    // otherwise -- it would only ever be drawn invisibly under "fused".
    drawProjectedPath(projected.groundTruth, ::toOffset, TrackColors.groundTruth, 6f)
    drawProjectedPath(projected.rawDeadReckoning, ::toOffset, TrackColors.rawDeadReckoning, 3f)
    drawProjectedPath(projected.fused, ::toOffset, TrackColors.fused, 5f)

    val trailLength = 30
    val trailStart = (currentIndex - trailLength).coerceAtLeast(0)
    for (i in trailStart..currentIndex) {
      val fraction =
        if (currentIndex == trailStart) 1f else (i - trailStart).toFloat() / (currentIndex - trailStart).toFloat()
      drawCircle(
        color = TrackColors.fused.copy(alpha = 0.15f + 0.6f * fraction),
        radius = 4f,
        center = toOffset(projected.fused[i]),
      )
    }
    val vehicleOffset = toOffset(projected.fused[currentIndex])
    drawCircle(color = TrackColors.fused, radius = 10f, center = vehicleOffset)
    drawCircle(color = Color.White, radius = 10f, center = vehicleOffset, style = Stroke(width = 2f))
  }
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
    LegendItem(TrackColors.groundTruth, "Ground truth")
    LegendItem(TrackColors.rawDeadReckoning, "Raw dead reckoning")
    LegendItem(TrackColors.fused, "Fused (final)")
  }
}

@Composable
private fun LegendItem(color: Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    Box(modifier = Modifier.size(10.dp).background(color = color, shape = CircleShape))
    Text(text = label, style = MaterialTheme.typography.labelSmall)
  }
}

@Composable
private fun StatusRow(point: TrackPoint, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxWidth()) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      StatusStat(label = "Speed", value = "%.1f m/s".format(point.speedPred))
      StatusStat(label = "Heading", value = "%.0f°".format(point.headingDeg))
      StatusStat(label = "Confidence", value = "${(point.alpha * 100).toInt()}%")
    }
    Spacer(Modifier.height(8.dp))
    val stateLabel = if (point.gpsAvailable) "GPS + Fusion" else "Dead Reckoning"
    val stateColor = if (point.gpsAvailable) TrackColors.fused else TrackColors.rawDeadReckoning
    AssistChip(
      onClick = {},
      label = { Text(stateLabel) },
      colors =
        AssistChipDefaults.assistChipColors(
          containerColor = stateColor.copy(alpha = 0.15f),
          labelColor = stateColor,
        ),
    )
  }
}

@Composable
private fun StatusStat(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = value, style = MaterialTheme.typography.titleMedium)
    Text(text = label, style = MaterialTheme.typography.labelSmall)
  }
}

@Composable
private fun PlaybackControls(
  currentIndex: Int,
  totalPoints: Int,
  isPlaying: Boolean,
  speedMultiplier: Int,
  onTogglePlay: () -> Unit,
  onSeek: (Int) -> Unit,
  onCycleSpeed: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Slider(
      value = currentIndex.toFloat(),
      onValueChange = { onSeek(it.toInt()) },
      valueRange = 0f..(totalPoints - 1).toFloat().coerceAtLeast(0f),
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Button(onClick = onTogglePlay) { Text(if (isPlaying) "Pause" else "Play") }
      TextButton(onClick = onCycleSpeed) { Text("${speedMultiplier}x") }
    }
  }
}
