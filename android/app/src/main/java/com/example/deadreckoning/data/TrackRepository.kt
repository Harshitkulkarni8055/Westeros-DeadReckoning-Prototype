package com.example.deadreckoning.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

interface TrackRepository {
  suspend fun loadTrack(): TrackData
}

/** Loads a recording's replay track from a bundled asset JSON file
 * (produced offline by `prototype/export_track.py`) -- there is no live
 * sensor or network data source in this prototype frontend.
 */
class AssetTrackRepository(
  private val context: Context,
  private val assetName: String = "S1_track.json",
) : TrackRepository {
  private val json = Json { ignoreUnknownKeys = true }

  override suspend fun loadTrack(): TrackData =
    withContext(Dispatchers.IO) {
      val text = context.assets.open(assetName).bufferedReader().use { it.readText() }
      json.decodeFromString(TrackData.serializer(), text)
    }
}
