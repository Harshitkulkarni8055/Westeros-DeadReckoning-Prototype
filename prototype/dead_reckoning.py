"""P1: heading estimation (gyro integration + magnetometer complementary
filter) and position integration (dead reckoning) from predicted speed
and heading.
"""
import numpy as np
import pandas as pd

from geo import wrap_angle

# Magnetometer axis/gyro-sign convention is phone-mount dependent (the
# proposal flags this as a calibration risk). We pick the best-matching
# convention against a short calibration window of ground truth heading
# rather than hardcoding one.
MAG_CANDIDATES = {
    "atan2(mx,my)": lambda mx, my: np.arctan2(mx, my),
    "atan2(my,mx)": lambda mx, my: np.arctan2(my, mx),
    "atan2(-mx,my)": lambda mx, my: np.arctan2(-mx, my),
    "atan2(mx,-my)": lambda mx, my: np.arctan2(mx, -my),
}


def _circular_mean_abs_error(a_rad, b_rad):
    return np.mean(np.abs(wrap_angle(a_rad - b_rad)))


def calibrate_conventions(df, calib_seconds=60):
    """Pick the gyro-integration sign and magnetometer axis convention that
    best match ground-truth heading over an initial calibration window —
    the smartphone analogue of the proposal's device calibration step.
    """
    calib = df[df.timestamp <= df.timestamp.iloc[0] + calib_seconds]
    heading_gt_rad = np.radians(calib.heading_gt_deg.values)

    best_sign, best_sign_err = 1, np.inf
    for sign in (1, -1):
        heading0 = heading_gt_rad[0]
        integrated = heading0 + np.cumsum(sign * calib.gyro_z.values * calib.dt.values)
        err = _circular_mean_abs_error(integrated, heading_gt_rad)
        if err < best_sign_err:
            best_sign, best_sign_err = sign, err

    best_mag_name, best_mag_err = None, np.inf
    for name, fn in MAG_CANDIDATES.items():
        mag_heading = fn(calib.mag_x.values, calib.mag_y.values)
        err = _circular_mean_abs_error(mag_heading, heading_gt_rad)
        if err < best_mag_err:
            best_mag_name, best_mag_err = name, err

    # Pick the complementary-filter gain that minimises heading error on
    # this same calibration window -- includes beta=0 (magnetometer
    # unreliable in-vehicle; pure gyro integration may win).
    best_beta, best_beta_err = 0.0, np.inf
    mag_fn = MAG_CANDIDATES[best_mag_name]
    mag_heading_calib = mag_fn(calib.mag_x.values, calib.mag_y.values)
    for beta in (0.0, 0.005, 0.01, 0.02, 0.05, 0.1):
        heading = np.empty(len(calib))
        heading[0] = heading_gt_rad[0]
        for i in range(1, len(calib)):
            predicted = heading[i - 1] + best_sign * calib.gyro_z.values[i] * calib.dt.values[i]
            correction = wrap_angle(mag_heading_calib[i] - predicted)
            heading[i] = predicted + beta * correction
        err = _circular_mean_abs_error(heading, heading_gt_rad)
        if err < best_beta_err:
            best_beta, best_beta_err = beta, err

    return {
        "gyro_sign": best_sign,
        "gyro_calib_mae_deg": np.degrees(best_sign_err),
        "mag_convention": best_mag_name,
        "mag_calib_mae_deg": np.degrees(best_mag_err),
        "beta": best_beta,
        "beta_calib_mae_deg": np.degrees(best_beta_err),
    }


def compute_heading(df, conventions=None, beta=None):
    """Complementary filter: integrate gyro yaw rate each step, then pull
    a small amount toward the magnetometer heading each step. Returns
    heading in radians, compass-bearing convention (0=N, +pi/2=E).
    """
    if conventions is None:
        conventions = calibrate_conventions(df)
    if beta is None:
        beta = conventions.get("beta", 0.02)
    gyro_sign = conventions["gyro_sign"]
    mag_fn = MAG_CANDIDATES[conventions["mag_convention"]]

    n = len(df)
    heading = np.empty(n)
    heading[0] = np.radians(df.heading_gt_deg.iloc[0])  # bootstrap fix, like an initial GPS course

    gyro_z = df.gyro_z.values
    dt = df.dt.values
    mag_heading = mag_fn(df.mag_x.values, df.mag_y.values)

    for i in range(1, n):
        predicted = heading[i - 1] + gyro_sign * gyro_z[i] * dt[i]
        correction = wrap_angle(mag_heading[i] - predicted)
        heading[i] = predicted + beta * correction

    return heading


def integrate_position(df, heading_rad, speed_mps, x0=0.0, y0=0.0):
    """Dead-reckon east/north displacement from speed + heading."""
    distance = speed_mps * df.dt.values
    dx = distance * np.sin(heading_rad)
    dy = distance * np.cos(heading_rad)
    x = x0 + np.cumsum(dx)
    y = y0 + np.cumsum(dy)
    return x, y


if __name__ == "__main__":
    from data_loader import find_recording, load_recording

    smartphone_csv, vehicle_csv = find_recording("S1")
    df = load_recording(smartphone_csv, vehicle_csv)
    conventions = calibrate_conventions(df)
    print("Calibrated heading conventions:", conventions)

    heading = compute_heading(df, conventions)
    heading_err_deg = np.degrees(np.abs(wrap_angle(heading - np.radians(df.heading_gt_deg.values))))
    print(f"Full-recording heading MAE vs ground truth: {heading_err_deg.mean():.1f} deg")
