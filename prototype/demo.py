"""Final prototype demo: runs the full pipeline on one IO-VNBD recording --
speed prediction -> heading estimation -> dead reckoning -> map snapping ->
GPS/INS fusion -- across a simulated GPS outage, and plots ground truth vs.
raw (uncorrected) dead-reckoning drift vs. the map-corrected/fused track.

This is the "done" deliverable described in PLAN.md.
"""
import argparse

import matplotlib.pyplot as plt
import numpy as np

from data_loader import find_recording, load_recording
from dead_reckoning import calibrate_conventions, compute_heading
from fusion import run_fused_pipeline
from geo import latlon_to_local_xy
from map_matcher import build_segment_index, download_road_graph, edges_local_xy
from speed_predictor import predict_speed, train_speed_predictor


def position_error(x_est, y_est, x_gt, y_gt):
    return np.sqrt((x_est - x_gt) ** 2 + (y_est - y_gt) ** 2)


def run_demo(recording_id, outage_start_s, outage_duration_s, pre_s, post_s, alpha_gps):
    """Run the full pipeline over one recording's outage window and return
    every intermediate array needed to plot it (demo.py) or export it
    (export_track.py) -- kept as the single source of truth for both.
    """
    smartphone_csv, vehicle_csv = find_recording(recording_id)
    df = load_recording(smartphone_csv, vehicle_csv)

    conventions = calibrate_conventions(df)
    model, speed_metrics = train_speed_predictor(df)
    pred_speed_full = predict_speed(model, df)
    print("Heading conventions:", conventions)
    print("Speed predictor metrics:", speed_metrics)

    win_start_s = outage_start_s - pre_s
    win_end_s = outage_start_s + outage_duration_s + post_s
    demo = df[(df.timestamp >= win_start_s) & (df.timestamp < win_end_s)].copy()

    heading = compute_heading(demo, conventions)
    pred_speed = pred_speed_full.loc[demo.index].values

    lat0, lon0 = demo.lat.iloc[0], demo.lon.iloc[0]
    gt_x, gt_y = latlon_to_local_xy(demo.lat, demo.lon, lat0, lon0)
    gt_x, gt_y = gt_x.values, gt_y.values

    # Simulated GPS: ground truth + realistic noise (~the phone's own
    # reported GPS ACCURACY of a few metres), at the full 10Hz rate. The
    # phone's *real* GPS column is only refreshed at ~1Hz and holds its
    # last value in between, which -- at 10Hz dead-reckoning steps -- reads
    # as a sawtooth artifact having nothing to do with the outage being
    # demonstrated. The proposal itself suggests this synthetic-noise
    # approach for benchmarking (section 3.3: "artificially drop GPS...").
    rng = np.random.default_rng(0)
    gps_noise_std = 5.0
    gps_x = gt_x + rng.normal(0, gps_noise_std, size=len(demo))
    gps_y = gt_y + rng.normal(0, gps_noise_std, size=len(demo))

    t_rel = demo.timestamp.values - demo.timestamp.values[0]
    gps_available = ~((t_rel >= pre_s) & (t_rel < pre_s + outage_duration_s))

    graph = download_road_graph(df.lat.min(), df.lat.max(), df.lon.min(), df.lon.max(), cache_name=recording_id)
    segments = edges_local_xy(graph, lat0, lon0)
    tree, seg_ids = build_segment_index(segments)

    result = run_fused_pipeline(
        demo, heading, pred_speed, gps_x, gps_y, gt_x, gt_y,
        gps_available, segments, tree, seg_ids, alpha_gps=alpha_gps,
    )

    return {
        "recording_id": recording_id,
        "lat0": lat0, "lon0": lon0,
        "t_rel": t_rel,
        "heading": heading,
        "pred_speed": pred_speed,
        "gt_x": gt_x, "gt_y": gt_y,
        "gps_x": gps_x, "gps_y": gps_y,
        "gps_available": gps_available,
        "alpha_gps": alpha_gps,
        "pre_s": pre_s, "outage_start_s": outage_start_s, "outage_duration_s": outage_duration_s,
        "win_start_s": win_start_s, "win_end_s": win_end_s,
        **result,
    }


def main(recording_id, outage_start_s, outage_duration_s, pre_s, post_s, alpha_gps):
    d = run_demo(recording_id, outage_start_s, outage_duration_s, pre_s, post_s, alpha_gps)
    gt_x, gt_y, t_rel = d["gt_x"], d["gt_y"], d["t_rel"]
    gps_available, result = d["gps_available"], d
    pre_s, outage_duration_s = d["pre_s"], d["outage_duration_s"]

    err_fused = position_error(result["fused_x"], result["fused_y"], gt_x, gt_y)
    outage_mask = ~gps_available
    print(f"\nGPS outage: {outage_duration_s:.0f}s starting at t={outage_start_s:.0f}s "
          f"(demo window: {d['win_start_s']:.0f}s to {d['win_end_s']:.0f}s)")
    print(f"Fused/corrected error -- overall: mean {err_fused.mean():.1f} m, max {err_fused.max():.1f} m")
    print(f"Fused/corrected error -- during outage: mean {err_fused[outage_mask].mean():.1f} m, "
          f"final {err_fused[outage_mask][-1]:.1f} m")

    valid_raw = ~np.isnan(result["raw_dr_x"])
    if valid_raw.any():
        err_raw = position_error(result["raw_dr_x"][valid_raw], result["raw_dr_y"][valid_raw],
                                  gt_x[valid_raw], gt_y[valid_raw])
        print(f"Raw uncorrected DR error (from outage start onward): mean {err_raw.mean():.1f} m, "
              f"final {err_raw[-1]:.1f} m")

    fig, axes = plt.subplots(1, 2, figsize=(13, 6))

    ax = axes[0]
    ax.plot(gt_x, gt_y, label="ground truth", linewidth=2)
    ax.plot(result["raw_dr_x"], result["raw_dr_y"], label="raw dead reckoning (no correction)", linestyle=":")
    ax.plot(result["fused_x"], result["fused_y"], label="map-corrected + fused", linestyle="--")
    ax.scatter(gt_x[0], gt_y[0], c="g", zorder=5, label="start")
    ax.set_title("Position track")
    ax.set_xlabel("east (m)")
    ax.set_ylabel("north (m)")
    ax.axis("equal")
    ax.legend()

    ax = axes[1]
    ax.plot(t_rel, err_fused, label="map-corrected + fused error")
    if valid_raw.any():
        ax.plot(t_rel[valid_raw], err_raw, label="raw DR error")
    ax.axvspan(pre_s, pre_s + outage_duration_s, color="red", alpha=0.1, label="GPS outage")
    ax.set_title("Position error vs. ground truth")
    ax.set_xlabel("time (s)")
    ax.set_ylabel("error (m)")
    ax.legend()

    fig.suptitle(f"IO-VNBD {recording_id} — GPS-denied navigation demo")
    fig.tight_layout()
    out_path = f"data/{recording_id}_demo.png"
    fig.savefig(out_path, dpi=150)
    print(f"\nsaved {out_path}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("recording_id", nargs="?", default="S1")
    parser.add_argument("--outage-start", type=float, default=2240.0)
    parser.add_argument("--outage-duration", type=float, default=30.0)
    parser.add_argument("--pre-seconds", type=float, default=15.0)
    parser.add_argument("--post-seconds", type=float, default=15.0)
    parser.add_argument("--alpha-gps", type=float, default=0.9)
    args = parser.parse_args()
    main(args.recording_id, args.outage_start, args.outage_duration, args.pre_seconds, args.post_seconds, args.alpha_gps)
