# PLAN: Dead Reckoning Prototype (SIH26168)

Source: `SIH 2026 Proposal.docx` (SIH 2026, Problem Statement SIH26168)

**Scope: PROTOTYPE, not the production app.** Goal is to prove the core
idea end-to-end on recorded data — IMU → predicted speed → dead-reckoning
position → map-snapped position → fused with GPS — and show it visually.
No Android app, no multi-phone support, no production hardening. Cut
anything that doesn't serve "does the core pipeline work and can we show
it."

## Repo layout to set up

```
sih-dead-reckoning/
  data/                 # IO-VNBD, one or two recordings is enough
  notebooks/            # exploratory analysis + the prototype itself
  prototype/            # small Python scripts for the pipeline
    data_loader.py      # parses IO-VNBD S-*.csv/V-*.csv into one dataframe
    geo.py               # shared lat/lon <-> local-xy projection helpers
    speed_predictor.py  # windowed-feature regressor (random forest)
    dead_reckoning.py   # heading (gyro+mag) + position integration
    map_matcher.py      # basic snap-to-road (nearest segment, not full HMM)
    fusion.py           # simple fixed-alpha GNSS/INS blend + outage sim
    plot_raw.py          # P0 sanity-check plot
    demo_p1.py, demo_p2.py  # milestone checkpoint plots (P1, P2)
    demo.py             # final pipeline demo (P3) -- the deliverable
  PLAN.md               # this file
```

## Milestones (prototype only)

### P0 — Data (Day 1)
- [x] Clone IO-VNBD (`github.com/onyekpeu/IO-VNBD`), grab 1–2 recordings
      (using the "Synchronised V and S datasets" — smartphone `S-*.csv` +
      vehicle ground truth `V-*.csv`, e.g. recording S1)
- [x] Parse into a dataframe (timestamp, accel_xyz, gyro_xyz, mag_xyz,
      lat/lon ground truth, velocity_gt) — `prototype/data_loader.py`
- [x] Plot raw IMU vs. ground-truth trajectory — `prototype/plot_raw.py`
      (see `data/S1_raw_overview.png`)

### P1 — Speed + dead reckoning (Days 2–3)
- [x] Speed predictor: started with linear regression on windowed
      accel/gyro features (`prototype/speed_predictor.py`) — MAE ~2.7 m/s,
      R^2 ~0.46 on a held-out 30% tail of the recording. Later upgraded to
      a random forest on the same features during P3 (see below) once it
      turned out to be the bottleneck; CNN-LSTM remains out of scope.
- [x] Heading: gyro integration + magnetometer complementary filter
      (`prototype/dead_reckoning.py`). Gyro sign, magnetometer axis
      convention, and the filter gain (beta) are all auto-calibrated
      against a short ground-truth window rather than hardcoded — the
      calibration picked beta=0 (pure gyro), since in-vehicle magnetometer
      readings were too noisy/interference-prone to help, a real limitation
      the proposal itself flags.
- [x] Integrate speed + heading into a position track; plot it against
      ground truth to show the drift the proposal describes —
      `prototype/demo_p1.py` (see `data/S1_p1_drift.png`). Over a clean
      2-minute held-out window, dead reckoning tracks well for ~55s then
      diverges to ~500m error after a sharp real-world turn it can't
      capture — the motivating problem for P2.

### P2 — Map snapping (Day 4)
- [x] Pull the OSM road graph for the recording's area (OSMnx) —
      `prototype/map_matcher.py::download_road_graph` (cached under
      `data/osm_cache/`)
- [x] Simplest useful version: snap each estimated point to its nearest
      road segment via a KD-tree candidate search + point-to-segment
      projection (skipped full HMM/Viterbi, per scope)
- [x] Plot corrected track vs. raw dead-reckoning track vs. ground truth —
      `prototype/demo_p2.py` (see `data/S1_p2_mapmatch.png`). Finding
      worth recording: naive nearest-segment snapping only corrects
      *lateral* drift, not *along-track* drift — on a near-straight road
      it barely helps (drift is mostly longitudinal there), but on a
      curving stretch it cuts heading-driven drift from ~27m to ~9m mean
      error. This is exactly the gap the proposal's HMM/Viterbi map
      matching (out of scope here) is meant to close.

### P3 — Fusion + demo (Day 5)
- [x] Simulate a GPS outage window in the recording — `prototype/demo.py`.
      Simulated GPS = ground truth + synthetic ~5m noise at the full 10Hz
      rate (the phone's *real* GPS column only refreshes at ~1Hz and holds
      stale values in between, which reads as an artifact, not signal;
      the proposal itself suggests synthetic GPS dropout for benchmarking).
- [x] Simple fixed-weight blend of GPS/dead-reckoning (`prototype/fusion.py`,
      `alpha_gps=0.9` when GPS is available; pure map-snapped DR when not) —
      learned fusion network stays out of scope.
- [x] `demo.py`: runs the full pipeline (speed predictor -> heading ->
      dead reckoning -> map snapping -> fusion) on recording S1 across a
      30s simulated GPS outage. See `data/S1_demo.png`: fused/corrected
      error stays ~5-15m throughout (near GPS-noise level) while raw,
      uncorrected dead reckoning grows to ~95m over the same 60s window —
      the core idea, visibly demonstrated.
- [x] Along the way, found the plan's starting linear-regression speed
      predictor (P1) had a systematic bias large enough to swamp any
      benefit from map-matching over even a 30-60s outage; swapped it for
      a random forest (`prototype/speed_predictor.py`) — still a plain
      scikit-learn model, not the proposal's CNN-LSTM, but MAE dropped
      from ~2.7 to ~1.5 m/s and R^2 from ~0.46 to ~0.77.

## Prototype status: COMPLETE
All of P0-P3 are done. `prototype/demo.py` is the single entry point that
reproduces the "done" deliverable below.

## Explicitly out of scope for the prototype
- Android/Kotlin app, mobile sensor integration
- TensorFlow Lite export, quantization, on-device inference timing
- CNN-LSTM speed predictor (only if the simple model is visibly too weak)
- Full HMM map-matching with Viterbi decoding
- Learned GNSS/INS fusion network
- Multi-phone calibration, real-world field testing
- These stay in the original full-system proposal for later — not part of
  this prototype's deliverable.

## What "done" looks like
A script/notebook that takes one IO-VNBD recording, simulates a GPS
blackout, and plots: ground truth vs. raw dead-reckoning drift vs.
map-corrected track — visibly demonstrating the core idea works.

## Immediate next step
Prototype is complete (see status above). Natural next steps if this gets
approved for further work: run `demo.py` on a second recording to check
the pipeline generalizes, or start pulling back in the full-system
milestones (CNN-LSTM, HMM/Viterbi, learned fusion, mobile app).

---
*This is a living plan — update checkboxes as the prototype progresses.
The full-system milestones (mobile app, learned fusion, HMM, quantization)
can be pulled back in later if the prototype is approved for full build-out.*
