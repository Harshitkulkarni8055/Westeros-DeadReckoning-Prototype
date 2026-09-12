"""Train a small Keras MLP on the same windowed-IMU features used by
speed_predictor.py's random forest, and export it to TensorFlow Lite for
the Android app. The random forest can't be exported to TFLite (that
format is TensorFlow-specific); this is the on-device equivalent.
"""
import numpy as np
import tensorflow as tf
from sklearn.metrics import mean_absolute_error, r2_score

from data_loader import find_recording, load_recording
from speed_predictor import FEATURE_COLS, build_features, train_test_split_temporal

OUT_DIR = "android/app/src/main/assets"


def main(recording_id="S1"):
    smartphone_csv, vehicle_csv = find_recording(recording_id)
    df = load_recording(smartphone_csv, vehicle_csv)
    feats = build_features(df)
    data = feats.join(df.velocity_gt).dropna()
    train, test = train_test_split_temporal(data)

    x_train, y_train = train[FEATURE_COLS].values, train.velocity_gt.values
    x_test, y_test = test[FEATURE_COLS].values, test.velocity_gt.values

    # Normalize features -- the raw scales differ a lot (e.g. gyro_yaw_std
    # ~0.1 vs mean_accel_mag ~10), which a small MLP trains far better with.
    mean, std = x_train.mean(axis=0), x_train.std(axis=0)
    std[std == 0] = 1.0
    x_train_n = (x_train - mean) / std
    x_test_n = (x_test - mean) / std

    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(len(FEATURE_COLS),)),
        tf.keras.layers.Dense(32, activation="relu"),
        tf.keras.layers.Dense(16, activation="relu"),
        tf.keras.layers.Dense(1),
    ])
    model.compile(optimizer="adam", loss="mae")
    model.fit(x_train_n, y_train, validation_data=(x_test_n, y_test),
              epochs=60, batch_size=64, verbose=2)

    pred_test = model.predict(x_test_n, verbose=0).flatten()
    print(f"\nKeras MLP -- test MAE: {mean_absolute_error(y_test, pred_test):.3f}, "
          f"R^2: {r2_score(y_test, pred_test):.3f}")

    # Fold normalization into the graph so the Android side just feeds raw
    # features -- no separate mean/std bookkeeping on the Kotlin side.
    inputs = tf.keras.Input(shape=(len(FEATURE_COLS),), name="imu_features")
    x = (inputs - mean.astype(np.float32)) / std.astype(np.float32)
    outputs = model(x)
    full_model = tf.keras.Model(inputs, outputs)

    converter = tf.lite.TFLiteConverter.from_keras_model(full_model)
    tflite_model = converter.convert()

    import os
    os.makedirs(OUT_DIR, exist_ok=True)
    out_path = os.path.join(OUT_DIR, "speed_predictor.tflite")
    with open(out_path, "wb") as f:
        f.write(tflite_model)
    print(f"saved {out_path} ({len(tflite_model)} bytes)")
    print("feature order (must match Kotlin side):", FEATURE_COLS)


if __name__ == "__main__":
    main()
