"""P1 demo: predicted speed + estimated heading -> dead-reckoned position
track, plotted against ground truth to show the drift the proposal
describes (motivating P2's map-matching correction).
"""
import argparse

import matplotlib.pyplot as plt
import numpy as np

from data_loader import find_recording, load_recording
from dead_reckoning import calibrate_conventions, compute_heading, integrate_position
from geo import latlon_to_local_xy
from speed_predictor import predict_speed, train_speed_predictor, train_test_split_temporal


def position_error(x_est, y_est, x_gt, y_gt):
    return np.sqrt((x_est - x_gt) ** 2 + (y_est - y_gt) ** 2)


def find_clean_window(df, window_len, step=50):
    """Locate the window (by row offset) with the highest minimum speed --
    i.e. the most stop-free stretch of driving. Ground-truth heading is
    noisy/meaningless near zero speed (GPS course-over-ground is
    undefined when stationary), so this gives the clearest drift demo.
    """
    v = df.velocity_gt.values
    n = len(v)
    best_offset, best_minv = 0, -1
    for i in range(0, n - window_len, step):
        minv = v[i:i + window_len].min()
        if minv > best_minv:
            best_minv, best_offset = minv, i
    return best_offset


def main(recording_id, demo_minutes):
    smartphone_csv, vehicle_csv = find_recording(recording_id)
    df = load_recording(smartphone_csv, vehicle_csv)

    # Gyro-sign / magnetometer-axis convention is a one-time device
    # calibration, done once on an early window of the whole recording.
    conventions = calibrate_conventions(df)
    print("Heading conventions:", conventions)

    # Speed predictor trained on the first 70%, evaluated out-of-sample on
    # the last 30% -- the demo window below is taken from that held-out part.
    model, metrics = train_speed_predictor(df)
    print("Speed predictor metrics:", metrics)
    pred_speed = predict_speed(model, df)

    _, test = train_test_split_temporal(df)
    demo_len = int(demo_minutes * 60 / df.dt.mean())
    offset = find_clean_window(test, demo_len)
    demo_start = test.index[offset]
    demo = df.loc[demo_start:demo_start + demo_len - 1]
    # Heading resets to the ground-truth fix at the start of the window,
    # like the last known heading before a GPS outage, then free-runs on
    # gyro+mag alone for the rest of the window -- this isolates the
    # drift accumulated *during* the outage rather than drift already
    # accumulated over the whole recording beforehand.
    demo_heading = compute_heading(demo, conventions)
    demo_pred_speed = pred_speed.loc[demo.index].values

    x_gt, y_gt = latlon_to_local_xy(demo.lat, demo.lon)
    x_gt, y_gt = x_gt.values, y_gt.values

    x_dr_pred, y_dr_pred = integrate_position(demo, demo_heading, demo_pred_speed, x_gt[0], y_gt[0])
    x_dr_true, y_dr_true = integrate_position(demo, demo_heading, demo.velocity_gt.values, x_gt[0], y_gt[0])

    err_pred = position_error(x_dr_pred, y_dr_pred, x_gt, y_gt)
    err_true = position_error(x_dr_true, y_dr_true, x_gt, y_gt)
    print(f"Demo window: {demo_minutes} min starting at t={demo.timestamp.iloc[0]:.0f}s")
    print(f"DR (predicted speed) position error: mean {err_pred.mean():.1f} m, final {err_pred[-1]:.1f} m")
    print(f"DR (ground-truth speed) position error: mean {err_true.mean():.1f} m, final {err_true[-1]:.1f} m")

    fig, axes = plt.subplots(1, 2, figsize=(13, 6))

    ax = axes[0]
    ax.plot(x_gt, y_gt, label="ground truth", linewidth=2)
    ax.plot(x_dr_true, y_dr_true, label="dead reckoning (true speed)", linestyle="--")
    ax.plot(x_dr_pred, y_dr_pred, label="dead reckoning (predicted speed)", linestyle=":")
    ax.scatter(x_gt[0], y_gt[0], c="g", zorder=5, label="start")
    ax.set_title(f"Position track — {demo_minutes} min window")
    ax.set_xlabel("east (m)")
    ax.set_ylabel("north (m)")
    ax.axis("equal")
    ax.legend()

    ax = axes[1]
    t = demo.timestamp.values - demo.timestamp.values[0]
    ax.plot(t, err_true, label="DR (true speed) error")
    ax.plot(t, err_pred, label="DR (predicted speed) error")
    ax.set_title("Position error vs. ground truth over time")
    ax.set_xlabel("time (s)")
    ax.set_ylabel("error (m)")
    ax.legend()

    fig.suptitle(f"IO-VNBD {recording_id} — P1 dead-reckoning drift demo")
    fig.tight_layout()
    out_path = f"data/{recording_id}_p1_drift.png"
    fig.savefig(out_path, dpi=150)
    print(f"saved {out_path}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("recording_id", nargs="?", default="S1")
    parser.add_argument("--minutes", type=float, default=10.0)
    args = parser.parse_args()
    main(args.recording_id, args.minutes)
