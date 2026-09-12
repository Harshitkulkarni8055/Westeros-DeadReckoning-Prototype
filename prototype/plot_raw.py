"""P0: plot raw IMU signals against the ground-truth trajectory for one
IO-VNBD recording, to sanity-check the parsed data before building the
dead-reckoning pipeline.
"""
import argparse

import matplotlib.pyplot as plt
import numpy as np

from data_loader import find_recording, load_recording
from geo import latlon_to_local_xy


def main(recording_id):
    smartphone_csv, vehicle_csv = find_recording(recording_id)
    df = load_recording(smartphone_csv, vehicle_csv)

    accel_mag = np.sqrt(df.accel_x**2 + df.accel_y**2 + df.accel_z**2)
    x, y = latlon_to_local_xy(df.lat, df.lon)

    fig, axes = plt.subplots(2, 2, figsize=(12, 9))

    ax = axes[0, 0]
    ax.plot(df.timestamp, accel_mag, linewidth=0.5)
    ax.set_title("Accelerometer magnitude")
    ax.set_xlabel("time (s)")
    ax.set_ylabel("m/s^2")

    ax = axes[0, 1]
    ax.plot(df.timestamp, df.gyro_z, linewidth=0.5)
    ax.set_title("Gyroscope yaw rate")
    ax.set_xlabel("time (s)")
    ax.set_ylabel("rad/s")

    ax = axes[1, 0]
    ax.plot(df.timestamp, df.velocity_gt, linewidth=0.8)
    ax.set_title("Ground-truth velocity")
    ax.set_xlabel("time (s)")
    ax.set_ylabel("m/s")

    ax = axes[1, 1]
    ax.plot(x, y, linewidth=0.8)
    ax.scatter(x.iloc[0], y.iloc[0], c="g", label="start", zorder=5)
    ax.scatter(x.iloc[-1], y.iloc[-1], c="r", label="end", zorder=5)
    ax.set_title("Ground-truth trajectory (local xy)")
    ax.set_xlabel("east (m)")
    ax.set_ylabel("north (m)")
    ax.axis("equal")
    ax.legend()

    fig.suptitle(f"IO-VNBD recording {recording_id} — raw IMU vs ground truth")
    fig.tight_layout()

    out_path = f"data/{recording_id}_raw_overview.png"
    fig.savefig(out_path, dpi=150)
    print(f"saved {out_path}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("recording_id", nargs="?", default="S1")
    args = parser.parse_args()
    main(args.recording_id)
