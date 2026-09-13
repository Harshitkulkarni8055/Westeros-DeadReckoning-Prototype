package com.example.deadreckoning.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.deadreckoning.data.AssetTrackRepository
import com.example.deadreckoning.data.ReplayCatalog
import com.example.deadreckoning.data.ReplayEntry
import com.example.deadreckoning.data.TrackData
import com.example.deadreckoning.theme.DriftBodyText
import com.example.deadreckoning.theme.DriftCardLight
import com.example.deadreckoning.theme.DriftNavyText
import com.example.deadreckoning.theme.DriftSurfaceLight
import com.example.deadreckoning.ui.common.DriftBottomBar
import com.example.deadreckoning.ui.common.MainTab
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Restyled after figmapics/Untitled5.png / Untitled6.png, but genuinely
 * real where the mock was staged: real OpenStreetMap tiles (osmdroid, no
 * API key needed) with the SELECTED replay's actual recorded ground-truth
 * path drawn on them, and real distance/duration numbers computed from
 * that recording -- not a fake "Riverside Park" pin. There is still no
 * live position marker here; that stays PlaybackScreen's job (see
 * FIGMA_REDESIGN_PLAN.md) -- this screen previews a route, it doesn't
 * play one back.
 */
@Composable
fun MapScreen(
  currentTab: MainTab,
  onTabSelected: (MainTab) -> Unit,
  onReplayRoute: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var selected by remember { mutableStateOf(ReplayCatalog.entries.first()) }
  var track by remember(selected) { mutableStateOf<TrackData?>(null) }

  LaunchedEffect(selected) { track = AssetTrackRepository(context.applicationContext, selected.assetFileName).loadTrack() }

  Scaffold(
    modifier = modifier,
    containerColor = DriftSurfaceLight,
    bottomBar = { DriftBottomBar(current = currentTab, onSelect = onTabSelected) },
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
      Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
        OsmMapView(track = track, modifier = Modifier.fillMaxSize())
        RouteChips(selected = selected, onSelect = { selected = it }, modifier = Modifier.align(Alignment.TopCenter).padding(12.dp))
      }
      track?.let { RouteSummaryCard(entry = selected, track = it, onReplay = { onReplayRoute(selected.assetFileName) }) }
    }
  }
}

@Composable
private fun OsmMapView(track: TrackData?, modifier: Modifier = Modifier) {
  AndroidView(
    modifier = modifier,
    factory = { ctx ->
      Configuration.getInstance().apply {
        // NOT ctx.packageName: OSM's tile servers explicitly denylist the
        // "com.example.*" User-Agent since it's the most common unchanged
        // default from copy-pasted tutorials (see osm.wiki/Blocked) --
        // any distinct, identifying string avoids that block.
        userAgentValue = "DriftNavPrototype-SIH26168/1.0"
        osmdroidBasePath = ctx.cacheDir
        osmdroidTileCache = ctx.cacheDir
      }
      MapView(ctx).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        controller.setZoom(14.0)
      }
    },
    onRelease = { it.onDetach() },
    update = { mapView ->
      mapView.overlays.clear()
      if (track != null && track.rows.isNotEmpty()) {
        val points = track.rows.map { GeoPoint(it.latGt, it.lonGt) }
        mapView.overlays.add(
          Polyline().apply {
            setPoints(points)
            outlinePaint.color = android.graphics.Color.parseColor("#1565C0")
            outlinePaint.strokeWidth = 8f
          },
        )
        mapView.overlays.add(Marker(mapView).apply { position = points.first(); title = "Start" })
        mapView.overlays.add(Marker(mapView).apply { position = points.last(); title = "End" })
        mapView.controller.setCenter(points[points.size / 2])
      }
      mapView.invalidate()
    },
  )
}

@Composable
private fun RouteChips(selected: ReplayEntry, onSelect: (ReplayEntry) -> Unit, modifier: Modifier = Modifier) {
  Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    ReplayCatalog.entries.forEach { entry ->
      FilterChip(selected = entry == selected, onClick = { onSelect(entry) }, label = { Text(entry.routeLabel) })
    }
  }
}

@Composable
private fun RouteSummaryCard(entry: ReplayEntry, track: TrackData, onReplay: () -> Unit) {
  val stats = remember(track) { routeStats(track) }
  Card(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    colors = CardDefaults.cardColors(containerColor = DriftCardLight),
    shape = RoundedCornerShape(20.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(entry.routeLabel, color = DriftNavyText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
      Text(entry.conditionLabel, color = DriftBodyText, fontSize = 13.sp)
      Spacer(Modifier.height(12.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Stat("%.1f km".format(stats.distanceKm), "Distance")
        Stat("%.0f s".format(stats.durationS), "Duration")
        Stat("${track.rows.size}", "Data points")
      }
      Spacer(Modifier.height(12.dp))
      Button(
        onClick = onReplay,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = DriftNavyText),
        shape = RoundedCornerShape(50),
      ) {
        Text("Replay this route  →", fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun Stat(value: String, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(value, color = DriftNavyText, fontWeight = FontWeight.Bold)
    Text(label, color = DriftBodyText, fontSize = 12.sp)
  }
}

private data class RouteStats(val distanceKm: Double, val durationS: Double)

private fun routeStats(track: TrackData): RouteStats {
  var meters = 0.0
  for (i in 1 until track.rows.size) {
    val a = track.rows[i - 1]
    val b = track.rows[i]
    meters += haversineMeters(a.latGt, a.lonGt, b.latGt, b.lonGt)
  }
  val durationS = track.rows.lastOrNull()?.t ?: 0.0
  return RouteStats(meters / 1000.0, durationS)
}

private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
  val r = 6371000.0
  val dLat = Math.toRadians(lat2 - lat1)
  val dLon = Math.toRadians(lon2 - lon1)
  val a =
    sin(dLat / 2) * sin(dLat / 2) +
      cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
  val c = 2 * atan2(sqrt(a), sqrt(1 - a))
  return r * c
}
