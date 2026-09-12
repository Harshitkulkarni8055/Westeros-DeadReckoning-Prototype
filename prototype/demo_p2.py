"""P2 demo: snap the drifting dead-reckoning track from P1 onto the OSM
road graph and compare corrected vs. raw DR vs. ground truth.
"""
import argparse

import matplotlib.pyplot as plt

from data_loader import find_recording, load_recording
from dead_reckoning import calibrate_conventions, compute_heading, integrate_position
from demo_p1 import find_clean_window, position_error
from geo import latlon_to_local_xy
from map_matcher import build_segment_index, download_road_graph, edges_local_xy, snap_track
from speed_predictor import predict_speed, train_speed_predictor, train_test_split_temporal


def main(recording_id, demo_minutes, first_seconds=None, start_seconds=None):
    smartphone_csv, vehicle_csv = find_recording(recording_id)
    df = load_recording(smartphone_csv, vehicle_csv)

    conventions = calibrate_conventions(df)
    model, metrics = train_speed_predictor(df)
    pred_speed = predict_speed(model, df)
    print("Heading conventions:", conventions)
    print("Speed predictor metrics:", metrics)

    demo_len = int(demo_minutes * 60 / df.dt.mean())
    if start_seconds is not None:
        # Explicit window -- e.g. a curvier stretch of road picked because
        # nearest-segment snapping mainly corrects *lateral* drift, so a
        # straight-road window (where drift is mostly along-track) won't
        # show much benefit. May overlap the speed predictor's training
        # data; that's fine here since this demo isolates map-matching's
        # effect on heading-driven drift, not speed-model generalization
        # (already reported separately via the held-out metrics above).
        demo_start = df.index[int(start_seconds / df.dt.mean())]
        demo = df.loc[demo_start:demo_start + demo_len - 1]
    else:
        _, test = train_test_split_temporal(df)
        offset = find_clean_window(test, demo_len)
        demo_start = test.index[offset]
        demo = df.loc[demo_start:demo_start + demo_len - 1]
    if first_seconds is not None:
        # Same starting fix/window as the P1 demo, but trimmed to the
        # early phase where drift is still small enough for nearest-
        # segment snapping to land on the correct road at all.
        keep = int(first_seconds / df.dt.mean())
        demo = demo.iloc[:keep]

    demo_heading = compute_heading(demo, conventions)
    demo_pred_speed = pred_speed.loc[demo.index].values

    lat0, lon0 = demo.lat.iloc[0], demo.lon.iloc[0]
    x_gt, y_gt = latlon_to_local_xy(demo.lat, demo.lon, lat0, lon0)
    x_gt, y_gt = x_gt.values, y_gt.values
    x_dr, y_dr = integrate_position(demo, demo_heading, demo_pred_speed, x_gt[0], y_gt[0])
    x_dr_true, y_dr_true = integrate_position(demo, demo_heading, demo.velocity_gt.values, x_gt[0], y_gt[0])

    graph = download_road_graph(df.lat.min(), df.lat.max(), df.lon.min(), df.lon.max(), cache_name=recording_id)
    segments = edges_local_xy(graph, lat0, lon0)
    tree, seg_ids = build_segment_index(segments)
    x_snap, y_snap, snap_dist = snap_track(x_dr, y_dr, segments, tree, seg_ids, max_dist=60.0)
    x_snap_true, y_snap_true, snap_dist_true = snap_track(x_dr_true, y_dr_true, segments, tree, seg_ids, max_dist=60.0)

    err_dr = position_error(x_dr, y_dr, x_gt, y_gt)
    err_snap = position_error(x_snap, y_snap, x_gt, y_gt)
    err_dr_true = position_error(x_dr_true, y_dr_true, x_gt, y_gt)
    err_snap_true = position_error(x_snap_true, y_snap_true, x_gt, y_gt)
    print(f"Demo window: {len(demo)*df.dt.mean():.0f}s starting at t={demo.timestamp.iloc[0]:.0f}s")
    print(f"Raw DR (predicted speed) error:        mean {err_dr.mean():.1f} m, final {err_dr[-1]:.1f} m")
    print(f"Map-snapped (predicted speed) error:   mean {err_snap.mean():.1f} m, final {err_snap[-1]:.1f} m")
    print(f"Raw DR (true speed, heading-only) error: mean {err_dr_true.mean():.1f} m, final {err_dr_true[-1]:.1f} m")
    print(f"Map-snapped (true speed) error:          mean {err_snap_true.mean():.1f} m, final {err_snap_true[-1]:.1f} m")
    print(f"(snap-to-road distance: mean {snap_dist.mean():.1f} m, max {snap_dist.max():.1f} m)")

    fig, axes = plt.subplots(1, 2, figsize=(13, 6))

    ax = axes[0]
    ax.plot(x_gt, y_gt, label="ground truth", linewidth=2)
    ax.plot(x_dr_true, y_dr_true, label="raw dead reckoning (heading drift)", linestyle=":")
    ax.plot(x_snap_true, y_snap_true, label="map-snapped", linestyle="--")
    ax.scatter(x_gt[0], y_gt[0], c="g", zorder=5, label="start")
    ax.set_title(f"Position track — {len(demo) * df.dt.mean():.0f}s window")
    ax.set_xlabel("east (m)")
    ax.set_ylabel("north (m)")
    ax.axis("equal")
    ax.legend()

    ax = axes[1]
    t = demo.timestamp.values - demo.timestamp.values[0]
    ax.plot(t, err_dr_true, label="raw DR error")
    ax.plot(t, err_snap_true, label="map-snapped error")
    ax.set_title("Position error vs. ground truth")
    ax.set_xlabel("time (s)")
    ax.set_ylabel("error (m)")
    ax.legend()

    fig.suptitle(f"IO-VNBD {recording_id} — P2 map-matching correction demo")
    fig.tight_layout()
    out_path = f"data/{recording_id}_p2_mapmatch.png"
    fig.savefig(out_path, dpi=150)
    print(f"saved {out_path}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("recording_id", nargs="?", default="S1")
    parser.add_argument("--minutes", type=float, default=1.0)
    parser.add_argument("--first-seconds", type=float, default=None)
    parser.add_argument("--start-seconds", type=float, default=2760.0)
    args = parser.parse_args()
    main(args.recording_id, args.minutes, args.first_seconds, args.start_seconds)
