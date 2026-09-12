"""Parse IO-VNBD synchronised smartphone (S-*.csv) + vehicle (V-*.csv) recordings
into a single clean dataframe for the dead-reckoning prototype.

The two files in a "Synchronised" recording folder have one row per sample at
the same 10Hz clock, so they are joined by row position.
"""
from pathlib import Path

import numpy as np
import pandas as pd

G = 9.80665  # m/s^2, used to convert km/hr -> m/s etc.


def _find_col(columns, keyword):
    keyword = keyword.lower()
    matches = [c for c in columns if keyword in c.strip().lower()]
    if not matches:
        raise KeyError(f"no column matching {keyword!r} in {list(columns)}")
    return matches[0]


def _load_smartphone_csv(path):
    df = pd.read_csv(path, encoding="latin-1")
    df.columns = [c.strip() for c in df.columns]
    col = lambda kw: _find_col(df.columns, kw)
    out = pd.DataFrame({
        "t_ms": df[col("TIME SINCE START")].astype(float),
        "phone_gps_lat": df[col("GPS LATITUDE")].astype(float),
        "phone_gps_lon": df[col("GPS LONGITUDE")].astype(float),
        "accel_x": df[col("ACCELEROMETER X")].astype(float),
        "accel_y": df[col("ACCELEROMETER Y")].astype(float),
        "accel_z": df[col("ACCELEROMETER Z")].astype(float),
        "gyro_x": df[col("GYROSCOPE Roll")].astype(float),
        "gyro_y": df[col("GYROSCOPE Pitch")].astype(float),
        "gyro_z": df[col("GYROSCOPE Yaw")].astype(float),
        "mag_x": df[col("MAGNETIC FIELD X")].astype(float),
        "mag_y": df[col("MAGNETIC FIELD Y")].astype(float),
        "mag_z": df[col("MAGNETIC FIELD Z")].astype(float),
    })
    return out


def _load_vehicle_csv(path):
    df = pd.read_csv(path, encoding="latin-1")
    df.columns = [c.strip() for c in df.columns]
    col = lambda kw: _find_col(df.columns, kw)
    out = pd.DataFrame({
        "t_s_of_day": df[col("Time Since Start of Day")].astype(float),
        "lat": df[col("Latitude")].astype(float),
        "lon": df[col("Longitude")].astype(float),
        "velocity_gt_kmh": df[col("Velocity")].astype(float),
        "heading_gt_deg": df[col("Heading")].astype(float),
    })
    return out


def load_recording(smartphone_csv, vehicle_csv):
    """Join a Synchronised S-*.csv / V-*.csv pair into one dataframe.

    Columns: timestamp (s, starts at 0), dt (s), accel_xyz (m/s^2),
    gyro_xyz (rad/s), mag_xyz (uT), lat/lon (ground truth, degrees),
    velocity_gt (m/s), heading_gt (degrees), phone_gps_lat/lon (degrees).
    """
    phone = _load_smartphone_csv(smartphone_csv)
    vehicle = _load_vehicle_csv(vehicle_csv)

    n = min(len(phone), len(vehicle))
    if len(phone) != len(vehicle):
        print(f"warning: row count mismatch ({len(phone)} vs {len(vehicle)}), truncating to {n}")
    phone = phone.iloc[:n].reset_index(drop=True)
    vehicle = vehicle.iloc[:n].reset_index(drop=True)

    df = pd.concat([phone, vehicle], axis=1)
    df["timestamp"] = (df["t_ms"] - df["t_ms"].iloc[0]) / 1000.0
    df["dt"] = df["timestamp"].diff().fillna(df["timestamp"].iloc[1] - df["timestamp"].iloc[0])
    df["velocity_gt"] = df["velocity_gt_kmh"] / 3.6

    cols = [
        "timestamp", "dt",
        "accel_x", "accel_y", "accel_z",
        "gyro_x", "gyro_y", "gyro_z",
        "mag_x", "mag_y", "mag_z",
        "lat", "lon", "velocity_gt", "heading_gt_deg",
        "phone_gps_lat", "phone_gps_lon",
    ]
    return df[cols]


def find_recording(recording_id, base_dir="data/IO-VNBD/Synchronised V abd S datasets/Categorised IOVNB Dataset"):
    """Locate the S-<id>.csv / V-<id>.csv pair for a recording id, e.g. 'S1'."""
    base = Path(base_dir)
    smartphone_csv = next(base.rglob(f"S-{recording_id}.csv"))
    vehicle_csv = next(base.rglob(f"V-{recording_id}.csv"))
    return smartphone_csv, vehicle_csv


if __name__ == "__main__":
    smartphone_csv, vehicle_csv = find_recording("S1")
    df = load_recording(smartphone_csv, vehicle_csv)
    print(df.describe())
    print(df.head())
