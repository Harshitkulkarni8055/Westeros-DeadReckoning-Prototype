package com.example.deadreckoning.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import android.net.ConnectivityManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.deadreckoning.theme.DriftBodyText
import com.example.deadreckoning.theme.DriftCardLight
import com.example.deadreckoning.theme.DriftNavyText
import com.example.deadreckoning.theme.DriftStatusGreen
import com.example.deadreckoning.theme.DriftStatusRed
import com.example.deadreckoning.theme.DriftSurfaceLight

private data class DeviceCheckItem(val label: String, val detail: String, val available: Boolean)

/** Restyled after figmapics/Untitled2.png / Untitled3.png, but every row
 * reflects a REAL check against this device -- a runtime location
 * permission request plus actual SensorManager hardware presence and
 * network state -- not fabricated checkmarks like the first redesign pass
 * skipped this screen for (see FIGMA_REDESIGN_PLAN.md's revised
 * decision). This prototype still doesn't use any of this data for live
 * navigation; it's an honest capability check, not a promise of live
 * functionality.
 */
@Composable
fun DeviceCheckScreen(onContinue: () -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  var locationGranted by
    remember {
      mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
          PackageManager.PERMISSION_GRANTED,
      )
    }
  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> locationGranted = granted }
  LaunchedEffect(Unit) {
    if (!locationGranted) permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
  }

  val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
  val locationManager = remember { context.getSystemService(LocationManager::class.java) }
  val connectivityManager = remember { context.getSystemService(ConnectivityManager::class.java) }

  val gpsProviderEnabled = remember { locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true }
  val hasImu =
    remember {
      sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null &&
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
    }
  val hasMagnetometer = remember { sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null }
  val hasOrientation = remember { sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null }
  val hasNetwork = remember { connectivityManager?.activeNetwork != null }

  val items =
    listOf(
      DeviceCheckItem("GNSS (GPS)", "Location services", locationGranted && gpsProviderEnabled),
      DeviceCheckItem("IMU Sensors", "Motion & orientation", hasImu),
      DeviceCheckItem("Magnetometer", "Compass & heading", hasMagnetometer),
      DeviceCheckItem("Device Orientation", "Rotation vector", hasOrientation),
      DeviceCheckItem("Network (optional)", "For live map tiles", hasNetwork),
    )

  Column(modifier = modifier.fillMaxSize().background(DriftSurfaceLight).safeDrawingPadding().padding(24.dp)) {
    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
      Text("Before we begin", color = DriftNavyText, fontWeight = FontWeight.Bold, fontSize = 26.sp)
      Spacer(Modifier.height(8.dp))
      Text(
        "A real check of this device's sensors -- it doesn't change what " +
          "the replay shows, this prototype still doesn't read live sensor " +
          "data for navigation.",
        color = DriftBodyText,
        fontSize = 14.sp,
        lineHeight = 20.sp,
      )
      Spacer(Modifier.height(20.dp))
      items.forEach { item ->
        DeviceCheckRow(item)
        Spacer(Modifier.height(12.dp))
      }
    }
    Spacer(Modifier.height(12.dp))
    Button(
      onClick = onContinue,
      modifier = Modifier.fillMaxWidth().height(56.dp),
      shape = RoundedCornerShape(50),
      colors = ButtonDefaults.buttonColors(containerColor = DriftNavyText),
    ) {
      Text("Continue  →", fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun DeviceCheckRow(item: DeviceCheckItem) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = DriftCardLight),
    shape = RoundedCornerShape(16.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(item.label, color = DriftNavyText, fontWeight = FontWeight.Bold)
        Text(item.detail, color = DriftBodyText, fontSize = 13.sp)
      }
      val badgeColor = if (item.available) DriftStatusGreen else DriftStatusRed
      Box(modifier = Modifier.size(28.dp).background(badgeColor, CircleShape), contentAlignment = Alignment.Center) {
        Text(if (item.available) "✓" else "✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
      }
    }
  }
}
