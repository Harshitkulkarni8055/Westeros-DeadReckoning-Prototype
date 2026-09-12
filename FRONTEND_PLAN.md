# PLAN: Prototype Android Frontend

Source: `SIH 2026 Proposal.docx` (Phase 3: Mobile App Development / UI-UX
section) + `PLAN.md` (the completed Python prototype backend).

**Scope: frontend for the PROTOTYPE, not the production app.** The backend
(`prototype/*.py`) already produces one complete result: recording S1, run
through speed prediction -> dead reckoning -> map snapping -> fusion,
across a simulated 30s GPS outage (`data/S1_demo.png`). There are no live
sensors, no TFLite model, and no on-device inference yet — that's the full
production system's job, not this prototype's.

So this app does **not** read the phone's accelerometer/GPS. It **replays
the S1 recording's precomputed track** on an Android screen, showing the
same map-view / speed / status / confidence UI the proposal describes for
the real app, driven by the data `demo.py` already computed. This proves
the frontend concept and gives a live, interactive demo — the same story
as the matplotlib plot, but on a phone, scrubbable, and moving.

## Why replay instead of live sensors
- The proposal's own "Early Prototyping" strategy is: build the Python
  prototype first, visualize dead reckoning vs ground truth, validate
  *before* mobile deployment (2.3 / Strategies for Success). We're one
  step further — visualizing on the actual target device, still before
  live sensors.
- Live capture needs permissions, a TFLite export of the speed model, and
  a Kotlin port of the DR/map-matching/fusion math — that's a much bigger
  effort explicitly out of scope for the backend prototype (see PLAN.md's
  "Explicitly out of scope" list) and should stay out of scope here too.
- A replay viewer still exercises every real frontend concern: map
  rendering, playback state, status derived from data, and the visual
  language (GPS/DR/Fused, confidence, drift) the real app will need later.

## Data bridge (Python -> Android)
The Android app needs one bundled file per recording, not the live
pipeline. New step in `prototype/`:

- `prototype/export_track.py` — runs the same pipeline as `demo.py` but
  dumps per-timestep rows to `data/S1_track.json` instead of only
  plotting. Columns needed per row:
  - `t` (seconds from start)
  - `lat_gt`, `lon_gt` (ground truth)
  - `lat_dr`, `lon_dr` (raw dead reckoning)
  - `lat_mm`, `lon_mm` (map-matched)
  - `lat_fused`, `lon_fused` (final fused output)
  - `speed_pred` (m/s, from the random-forest speed predictor)
  - `heading` (radians or degrees, pick one and note it)
  - `gps_available` (bool — false during the simulated outage window)
  - `alpha` (the fusion blend weight actually used at this step)
- That JSON gets copied to `android/app/src/main/assets/S1_track.json` and
  bundled into the app — no network access needed at runtime.

## Screens

### Playback screen (primary, replaces today's placeholder `MainScreen`)
- **Track view**: a Compose `Canvas` that draws all four paths (ground
  truth, raw DR, map-matched, fused) in distinct colors, normalized from
  lat/lon into screen space via a bounding-box projection (same idea as
  `prototype/geo.py`'s local-xy conversion, ported to Kotlin) — no map
  tiles, no API key, works fully offline. A vehicle marker moves along
  the fused track as playback advances, with a fading trail behind it.
- **Status row**: current speed (predicted), heading, and a state badge —
  "GPS" / "Dead Reckoning" / "Fused" — derived from `gps_available` at the
  current timestep, matching the proposal's UI/UX bullet list exactly.
- **Confidence indicator**: a simple 0-100% bar/ring driven by `alpha`.
- **Playback controls**: play/pause, a scrub slider over the full
  recording, and a speed multiplier (1x/2x/5x) so the ~60-90s window
  isn't a long wait to watch.
- **Legend**: color key for the four tracks (ground truth vs raw DR vs
  map-matched vs fused) so the drift story is legible at a glance.

### Summary screen (reached after playback ends, or via a button)
- Final error stats mirroring `S1_demo.png`'s takeaway: mean error for
  raw DR vs fused during the outage window, in meters. Static text/number
  readout — no new visualization needed, this just states the punchline
  of the demo in words for anyone who didn't watch the whole replay.

## Architecture
- `data/TrackRepository.kt` — loads and parses the bundled
  `S1_track.json` asset into a `List<TrackPoint>` data class. Replaces
  the current stub `DefaultDataRepository`.
- `ui/playback/PlaybackViewModel.kt` — holds `currentIndex`, `isPlaying`,
  `speedMultiplier` as state; a coroutine ticker advances `currentIndex`
  on a fixed interval while playing. Replaces `MainScreenViewModel`.
- `ui/playback/PlaybackScreen.kt` — the Canvas track view + status row +
  controls, composed from `PlaybackViewModel`'s state.
- `ui/summary/SummaryScreen.kt` — small stats screen, new `NavKey` added
  alongside the existing `Main` one in `Navigation.kt`/`NavigationKeys.kt`.
- Delete/replace the current placeholder `Greeting("Android")` UI — it's
  template scaffolding, not part of the app.

## Milestones

### F0 — Data bridge (fastest path to something on screen) — DONE
- [x] `prototype/export_track.py`: run pipeline, dump `S1_track.json`
      (reuses `demo.py`'s `run_demo()`, refactored out so both share one
      computation path)
- [x] Copy JSON into `android/app/src/main/assets/`
- [x] Kotlin `TrackPoint`/`TrackData` data classes (`data/TrackData.kt`) +
      `AssetTrackRepository` that parses it (`data/TrackRepository.kt`)
- [x] Wired into a ViewModel that surfaces load errors in the UI instead
      of just logging (`PlaybackUiState.Error`) — stronger than the
      original "log on launch" plan

### F1 — Static track rendering — DONE
- [x] Bounding-box lat/lon -> screen-space projection, Kotlin port of
      `geo.py` (`ui/playback/TrackProjection.kt`)
- [x] Canvas draws all four tracks, correctly scaled/color-coded
      (`TrackCanvas` in `ui/playback/PlaybackScreen.kt`) — not yet
      visually cross-checked against `data/S1_demo.png` on a real device
      (no emulator/build available in this pass, see note below)

### F2 — Playback — DONE
- [x] `PlaybackViewModel` ticker drives `currentIndex` forward, with
      pause/resume-from-start-at-end behavior
- [x] Vehicle marker + fading trail on the fused track
- [x] Scrub slider, play/pause, speed multiplier (1x/2x/5x) controls
- [x] Status row: live speed/heading/state badge from the current row

### F3 — Polish — partially done
- [x] Confidence indicator driven by `alpha` (shown as a % in the status row)
- [x] Legend for the four track colors
- [ ] Visual cue for the GPS-outage window on the canvas/timeline itself
      (currently only reflected in the per-row status badge, not drawn
      on the scrub bar or map)
- [ ] Summary screen with final error stats + nav route to reach it
      (`TrackData.summary` is already parsed and available, just not
      displayed anywhere yet)
- [ ] Basic light/dark theme pass (currently default Material3 theme)

**Verified**: builds and runs in Android Studio (tested on a Pixel 9a
API 37.1 emulator) — playback, the moving marker, and the live status
readouts all work as intended. One fix made after first run: the
map-matched track was drawn but never visible (see note below), so it was
dropped from the on-screen legend/rendering entirely; it's still exported
in the JSON, just not drawn.

**Known non-issue**: only 3 of the original 4 tracks are drawn (ground
truth, raw dead reckoning, fused). Map-matched was removed from the
canvas/legend because per `fusion.py`'s own math it is pixel-identical to
the fused track for the entire GPS-outage window and within ~5m of it
otherwise -- it was always being drawn invisibly underneath "fused".
`demo.py`'s original matplotlib plot never showed it separately either,
for the same reason.

## F4 — Multiple replays — DONE
- [x] Exported two more recordings the same way as S1 — `S3c` (clean
      calibration, strong result: fused mean error 14.9m vs raw 57.5m
      during the outage) and `S3a` (noisier heading calibration, still a
      real win but a smaller one: fused 41.7m vs raw 73.4m) — both via
      `prototype/export_track.py <id> --outage-start ... --outage-duration
      30 --pre-seconds 15 --post-seconds 15`, picked from recordings with
      clean (non-wrapping) timestamps. Copied to
      `android/app/src/main/assets/`.
- [x] `data/ReplayCatalog.kt` — hardcoded list of the three bundled
      replays (title, route label, asset filename); no dynamic discovery.
- [x] `ui/replaylist/ReplayListScreen.kt` — landing screen, one card per
      replay.
- [x] `Playback` nav key now carries `assetFileName`; `PlaybackScreen`
      loads whichever asset was picked, keyed by filename so switching
      replays gets a fresh `PlaybackViewModel`/ticker instead of reusing
      stale state. Added a "← All replays" button back to the list.
- [x] `MainNavigation` now starts at `ReplayList` instead of `Playback`.

## Explicitly out of scope for this frontend pass
- Live sensor capture (accelerometer/gyro/mag/GPS) and permissions
- TFLite model loading / on-device inference
- Real map tiles (osmdroid/Google Maps/Mapbox) — the Canvas plot is the
  map for this prototype
- Settings, accounts, persistence, anything beyond replay + summary
- These are real frontend work for the full production app later, not
  this prototype pass.

## What "done" looks like
Install the app, hit play, and watch the fused track hug ground truth
through the simulated outage while the raw dead-reckoning track visibly
drifts away — the same story as `S1_demo.png`, but live and on-device,
with speed/heading/status/confidence updating in real time as it plays.

---
*Companion to `PLAN.md` (backend, complete). Update checkboxes as this
progresses.*
