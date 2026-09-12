"""P1: simple speed predictor. Learns a linear regression from windowed
IMU statistics to ground-truth vehicle speed, standing in for the
proposal's CNN-LSTM speed predictor (out of scope for this prototype
unless this simple model is visibly too weak).
"""
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_absolute_error, r2_score

WINDOW = 20  # 2s @ 10Hz, matching the recording's sample rate


def build_features(df, window=WINDOW):
    """Causal (trailing) rolling-window features from raw IMU columns, so
    each row's features only use samples up to and including that row —
    the same information available in a real-time deployment.
    """
    accel_mag = np.sqrt(df.accel_x**2 + df.accel_y**2 + df.accel_z**2)
    gyro_mag = np.sqrt(df.gyro_x**2 + df.gyro_y**2 + df.gyro_z**2)

    feats = pd.DataFrame(index=df.index)
    feats["mean_accel_mag"] = accel_mag.rolling(window).mean()
    feats["std_accel_mag"] = accel_mag.rolling(window).std()
    feats["max_accel_z"] = df.accel_z.rolling(window).max()
    feats["min_accel_z"] = df.accel_z.rolling(window).min()
    feats["accel_jerk"] = accel_mag.diff().abs().rolling(window).mean()
    feats["mean_gyro_mag"] = gyro_mag.rolling(window).mean()
    feats["gyro_yaw_std"] = df.gyro_z.rolling(window).std()
    feats["mean_abs_gyro_z"] = df.gyro_z.abs().rolling(window).mean()
    return feats


FEATURE_COLS = [
    "mean_accel_mag", "std_accel_mag", "max_accel_z", "min_accel_z",
    "accel_jerk", "mean_gyro_mag", "gyro_yaw_std", "mean_abs_gyro_z",
]


def train_test_split_temporal(df, train_frac=0.7):
    """Contiguous temporal split (not shuffled) — train on the first part
    of the drive, test on a later, unseen part. Shuffling would leak
    information between overlapping rolling windows.
    """
    n = len(df)
    split = int(n * train_frac)
    return df.iloc[:split], df.iloc[split:]


def train_speed_predictor(df):
    feats = build_features(df)
    data = feats.join(df.velocity_gt).dropna()
    train, test = train_test_split_temporal(data)

    # Plain linear regression (the plan's starting point) had MAE ~2.7 m/s /
    # R^2 ~0.46, which is too weak: the along-track drift it introduces
    # over even a short GPS outage swamps everything map-matching can
    # correct (map-matching only fixes lateral drift, see PLAN.md P2). A
    # random forest is still a simple, classical scikit-learn model --
    # not the proposal's CNN-LSTM -- but nearly halves MAE.
    model = RandomForestRegressor(n_estimators=100, max_depth=10, random_state=0, n_jobs=-1)
    model.fit(train[FEATURE_COLS], train.velocity_gt)

    pred_train = model.predict(train[FEATURE_COLS])
    pred_test = model.predict(test[FEATURE_COLS])
    metrics = {
        "train_mae": mean_absolute_error(train.velocity_gt, pred_train),
        "test_mae": mean_absolute_error(test.velocity_gt, pred_test),
        "train_r2": r2_score(train.velocity_gt, pred_train),
        "test_r2": r2_score(test.velocity_gt, pred_test),
    }
    return model, metrics


def predict_speed(model, df):
    """Predict speed for every row of df; rows without a full window get
    NaN, which the caller should handle (e.g. by dropping the warm-up).
    """
    feats = build_features(df)
    pred = pd.Series(np.nan, index=df.index)
    valid = feats.dropna()
    pred.loc[valid.index] = model.predict(valid[FEATURE_COLS])
    return pred


if __name__ == "__main__":
    from data_loader import find_recording, load_recording

    smartphone_csv, vehicle_csv = find_recording("S1")
    df = load_recording(smartphone_csv, vehicle_csv)
    model, metrics = train_speed_predictor(df)
    print("Speed predictor (linear regression on windowed IMU features):")
    for k, v in metrics.items():
        print(f"  {k}: {v:.3f}")
