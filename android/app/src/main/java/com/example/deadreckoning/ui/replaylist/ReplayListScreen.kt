package com.example.deadreckoning.ui.replaylist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.deadreckoning.data.ReplayCatalog
import com.example.deadreckoning.data.ReplayEntry

/** Landing screen: pick one of the bundled replay recordings before
 * entering [com.example.deadreckoning.ui.playback.PlaybackScreen].
 */
@Composable
fun ReplayListScreen(onSelect: (ReplayEntry) -> Unit, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxSize()) {
    Text(text = "GPS-denied navigation replays", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(4.dp))
    Text(
      text = "Pick a recorded IO-VNBD route to replay the dead-reckoning demo.",
      style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(16.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      items(ReplayCatalog.entries) { entry -> ReplayCard(entry = entry, onClick = { onSelect(entry) }) }
    }
  }
}

@Composable
private fun ReplayCard(entry: ReplayEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Card(onClick = onClick, modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(text = entry.title, style = MaterialTheme.typography.titleMedium)
      Spacer(Modifier.height(2.dp))
      Text(text = entry.routeLabel, style = MaterialTheme.typography.bodyMedium)
    }
  }
}
