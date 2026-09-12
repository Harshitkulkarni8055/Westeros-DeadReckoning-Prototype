# Dead Reckoning Prototype (SIH26168)

A prototype for **AI/ML-Based Intelligent Dead Reckoning** (Smart India
Hackathon 2026, problem statement SIH26168): GPS-denied vehicle navigation
using phone IMU sensors, map matching, and GNSS/INS fusion.

This is a cut-down **proof of concept**, not the full production system
described in the proposal. It answers one question: *does the core idea
work end-to-end on real recorded data, and can we show it?* See
`PLAN.md` and `FRONTEND_PLAN.md` for the full scope and what's
deliberately left out.

## What's here

### Backend (Python) — complete
`prototype/` runs the full pipeline on one [IO-VNBD](https://github.com/onyekpeu/IO-VNBD)
recording (S1): speed prediction (random forest on windowed IMU features)
→ heading (gyro + magnetometer complementary filter) → dead reckoning →
map snapping (OSM road graph, nearest-segment) → GPS/INS fusion, across a
simulated 30-second GPS outage.

```
prototype/
  data_loader.py        # parses IO-VNBD into one dataframe
  geo.py                 # lat/lon <-> local-xy projection helpers
  speed_predictor.py     # windowed-feature random forest speed model
  dead_reckoning.py      # heading estimation + position integration
  map_matcher.py         # OSM road graph + nearest-segment snapping
  fusion.py               # fixed-alpha GPS/INS blend + outage simulation
  demo.py                 # runs the full pipeline, plots the result (P3 deliverable)
  export_track.py         # same pipeline, exports per-step JSON for the Android app
```

Run it:
```
pip install -r requirements.txt
python prototype/demo.py          # -> data/S1_demo.png
python prototype/export_track.py  # -> data/S1_track.json
```

The headline result: during the simulated outage, raw uncorrected dead
reckoning drifts to ~95m error while the map-corrected + fused track stays
within ~5-15m of ground truth (see `data/S1_demo.png`).

### Frontend (Android) — in progress
`android/` is a Kotlin/Compose app that **replays** precomputed tracks from
`export_track.py` on a phone screen — an interactive version of the same
story `S1_demo.png` tells, with a moving vehicle marker, live
speed/heading/confidence readouts, and a GPS/fusion status indicator. A
landing screen lists three bundled replays (recordings S1, S3c, S3a — each
a different route and outage window) to pick from. It does not read live
sensors or run any model on-device; see `FRONTEND_PLAN.md` for why and
what's still open (an outage-window highlight and a results summary
screen).

Open `android/` in Android Studio, let Gradle sync, run on an emulator or
device.

## Explicitly out of scope for this prototype
Live sensor capture, on-device inference (TFLite), full HMM/Viterbi map
matching, a learned GNSS/INS fusion network, and multi-phone calibration
all stay in the full-system proposal for later. See `PLAN.md`'s "out of
scope" section for the complete list and reasoning.
