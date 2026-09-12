# PLAN: Figma-Aligned Redesign ("Drift Nav")

Source: `figmapics/*.png` (10 screens exported from a teammate's Figma
prototype, "Drift Nav — GPS-Denied Navigation") + `FRONTEND_PLAN.md` (the
current replay-only Android app).

**Scope tension up front:** the Figma prototype designs the *full
production app* — live map tiles, live sensor permission checks, turn-by-
turn search — on top of the same replay/scenario concept we already
built. Our app only replays precomputed recordings (see `FRONTEND_PLAN.md`
"Explicitly out of scope": no live sensors, no real map tiles, no
on-device inference). This plan sorts each Figma screen into what we can
honestly adopt now vs. what would require pretending to have capabilities
we don't, and flags that tension everywhere it appears rather than papering
over it.

## What the Figma prototype shows (9 unique screens)
1. **Hero/landing** (`Untitled.png`) — dark navy, "DRIFT NAV" wordmark,
   headline "Navigate beyond GPS.", 3-icon flow (Onboard Sensors -> ML
   Models -> GNSS+INS Fusion), "Start Navigation" (solid white pill) +
   "Explore Demo" (outlined mint pill) buttons.
2. **"Before we begin" permission checklist** (`Untitled2.png` /
   `Untitled3.png`, duplicates) — light, list of sensor cards (GNSS, IMU,
   Magnetometer, Device Orientation, Network) each with a green checkmark,
   "Continue" button.
3. **"You're all set!"** (`Untitled4.png`) — dark, confirmation screen
   after the checklist, same two CTA buttons as the hero.
4. **Live map** (`Untitled5.png`) — satellite map, search bar, destination
   pin, "GPS + Fusion / Accurate positioning" status pill, zoom controls,
   route bottom-sheet preview, speed/heading/confidence stat tiles, 4-tab
   bottom bar (Map / Replay / System / More).
5. **Route detail sheet** (`Untitled6.png`) — modal over the map: distance/
   time, route type, "Start Navigation", "View on map".
6. **System Status** (`Untitled7.png`) — dark, "AI Fusion Active" pill,
   per-component rows (GNSS / IMU / Map Matching / Fusion Network) each
   with a colored dot and Active/Unavailable label.
7. **Test Scenarios** (`Untitled8.png`) — light, numbered cards ("01 Route
   A-B, GPS denied · 42 sec", "02 Route C-D...", "03 Route E-F, signal
   obstruction · 56 sec"), each with a "Replay" pill button. This is
   functionally our existing `ReplayListScreen` — same A-B/C-D/E-F naming
   we already picked independently.
8. **Scenario detail/playback** (`Untitled9.png`) — light, legend chips
   (Ground Truth / Raw DR / AI Fusion), a bordered canvas card with A/B
   endpoint markers, a "Position Estimation" stat card (confidence/speed/
   heading), and a scrub slider with a play button. This is our
   `PlaybackScreen`, restyled.
9. **"How it works" modal** (`Untitled10.png`) — dark, static pipeline
   diagram (GNSS/IMU/Vibration -> Fusion Network/Map Matching/Final
   Position), two stat chips ("10 Hz real-time", "<2MB edge model"),
   "Learn more" button.

## Design tokens (approximate — read off screenshots, not Figma Dev Mode;
## verify exact hex/spacing with your teammate before final polish)
- Dark screens: near-black navy background (~`#0B1220`), white headline
  text, mint/spring-green accent (~`#2ED9A7`) for the wordmark, active
  icons, and outlined-pill borders.
- Light screens: white/off-white background (~`#FFFFFF`–`#F7F9FC`), dark
  navy text/buttons (~`#14213D`), pale blue circular icon badges, green
  checkmarks, red status dot for "unavailable".
- Shape language everywhere: fully-rounded ("pill") buttons, large-radius
  cards (~20-24dp), circular icon containers.
- Bottom tab bar: 4 icons (Map / Replay / System / More), active tab
  tinted blue.

## Screen-by-screen scope call

| Figma screen | Call | Why |
|---|---|---|
| Hero/landing | **Adopt** | Just a static entry screen + navigation, no live data needed. |
| Permission checklist / "all set" | **Skip** | We don't read any sensors — checkmarks would be fabricated. Contradicts this repo's whole practice of labeling what's real vs. simulated (see `PLAN.md`/`FRONTEND_PLAN.md` callouts throughout). |
| Live map + search + route sheet | **Defer** | Needs a real map SDK (Mapbox/Google Maps + API key) and live GPS — explicitly out of scope for the replay-only prototype. |
| System Status | **Adapt** | Can build for real: derive each row from the *loaded replay's* actual data (e.g. GNSS row reflects `gps_available` at the current playback point) instead of hardcoding "Active". |
| Test Scenarios | **Adopt** | Direct restyle of our existing `ReplayListScreen` — same content, new look. |
| Scenario detail/playback | **Adopt** | Direct restyle of our existing `PlaybackScreen` — same content, new look. |
| "How it works" modal | **Adopt, reworded** | Static and low-risk, but its copy describes the full proposal's aspirational pipeline (CNN-LSTM, learned fusion network) — needs rewriting to describe what *this prototype* actually runs (random forest, fixed-alpha fusion), or explicitly label it as "the target architecture" vs. "what's running now". |
| Bottom tab bar (Map/Replay/System/More) | **Adapt** | Since Map is deferred, needs a decision — see Open Questions. |

## Proposed phased plan

### Phase 1 — Restyle the two screens we already have (low risk, high value)
- [ ] Pull the color/typography/shape tokens above into `theme/Color.kt` /
      `theme/Theme.kt` (dark navy + mint accent + light content surfaces)
- [ ] Restyle `ReplayListScreen` to match "Test Scenarios": numbered badge
      (01/02/03), "GPS denied · Ns" subtitle, "Replay" pill button
- [ ] Restyle `PlaybackScreen` to match the scenario-detail layout: legend
      as colored-dot chips row, canvas in a bordered card, a "Position
      Estimation" stat card below (reusing our existing confidence/speed/
      heading values), slider + play button restyled to match

### Phase 2 — Hero/landing screen (low risk)
- [ ] New entry screen (dark, "DRIFT NAV" wordmark, headline, 3-icon flow,
      "Explore Demo" -> Test Scenarios). Decide what "Start Navigation"
      does here — see Open Questions.

### Phase 3 — System Status screen (medium — must stay honest)
- [ ] New tab/screen showing pipeline component rows, but every row backed
      by real derived data from the currently-loaded replay, not a
      hardcoded "Active" — needs a decision on which replay's data drives
      it when nothing is actively playing.

### Phase 4 — "How it works" modal (low risk)
- [ ] Static info screen/modal, copy adjusted to distinguish "what this
      prototype does" from "what the full proposal targets".

### Explicitly deferred / out of scope for this pass
- Live map screen with real tiles, search, turn-by-turn routing
- Permission/sensor-check onboarding (nothing real to check yet)
- The "Map" tab itself, unless kept as a disabled placeholder (see below)

## Decisions (confirmed)
1. **Bottom tabs**: 3 tabs — Replay / System / More. No Map tab at all.
2. **Hero CTA**: only "Explore Demo" is shown; "Start Navigation" is
   dropped rather than shown disabled.
3. **System Status**: backed by real data from whichever replay is
   currently open (via a small shared `ActivePlaybackState` the
   `PlaybackViewModel` publishes to each tick) — GNSS flips to
   "Unavailable" only during that recording's actual simulated outage
   window. When nothing is playing, the screen shows an explicit idle
   state rather than fabricated status.
4. **"How it works" copy**: rewritten to describe this prototype's actual
   pipeline (random-forest speed predictor, gyro+mag complementary filter,
   nearest-segment map snap, fixed-alpha fusion) instead of the full
   proposal's aspirational CNN-LSTM/HMM/learned-fusion architecture.

---
Implementation in progress — see `FRONTEND_PLAN.md` for the phase
checklist as it's built out.
