"""F0 (FRONTEND_PLAN.md): export one recording's demo pipeline output to a
JSON file the Android app bundles as an asset and replays. Reuses
demo.py's run_demo() so the frontend replays exactly what demo.py plots --
no separate computation path to drift out of sync.
"""
import argparse
import json

import numpy as np

from demo import position_error, run_demo
from geo import local_xy_to_latlon


def build_rows(d):
    lat0, lon0 = d["lat0"], d["lon0"]

    lat_gt, lon_gt = local_xy_to_latlon(d["gt_x"], d["gt_y"], lat0, lon0)
    lat_dr, lon_dr = local_xy_to_latlon(d["raw_dr_x"], d["raw_dr_y"], lat0, lon0)
    lat_mm, lon_mm = local_xy_to_latlon(d["mapmatched_x"], d["mapmatched_y"], lat0, lon0)
    lat_fused, lon_fused = local_xy_to_latlon(d["fused_x"], d["fused_y"], lat0, lon0)

    err_fused = position_error(d["fused_x"], d["fused_y"], d["gt_x"], d["gt_y"])
    heading_deg = np.degrees(d["heading"]) % 360.0
    alpha = np.where(d["gps_available"], d["alpha_gps"], 0.0)

    rows = []
    for i in range(len(d["t_rel"])):
        # raw_dr_x/y is NaN before the outage starts (see fusion.py) --
        # omit lat/lon for those rows rather than exporting NaN into JSON.
        has_raw = not np.isnan(d["raw_dr_x"][i])
        rows.append({
            "t": round(float(d["t_rel"][i]), 2),
            "lat_gt": round(float(lat_gt[i]), 7), "lon_gt": round(float(lon_gt[i]), 7),
            "lat_dr": round(float(lat_dr[i]), 7) if has_raw else None,
            "lon_dr": round(float(lon_dr[i]), 7) if has_raw else None,
            "lat_mm": round(float(lat_mm[i]), 7), "lon_mm": round(float(lon_mm[i]), 7),
            "lat_fused": round(float(lat_fused[i]), 7), "lon_fused": round(float(lon_fused[i]), 7),
            "speed_pred": round(float(d["pred_speed"][i]), 2),
            "heading_deg": round(float(heading_deg[i]), 1),
            "gps_available": bool(d["gps_available"][i]),
            "alpha": round(float(alpha[i]), 2),
            "error_fused_m": round(float(err_fused[i]), 2),
        })
    return rows


def main(recording_id, outage_start_s, outage_duration_s, pre_s, post_s, alpha_gps, out_path):
    d = run_demo(recording_id, outage_start_s, outage_duration_s, pre_s, post_s, alpha_gps)
    rows = build_rows(d)

    err_fused = np.array([r["error_fused_m"] for r in rows])
    outage_mask = ~np.array([r["gps_available"] for r in rows])

    payload = {
        "recording_id": recording_id,
        "lat0": float(d["lat0"]), "lon0": float(d["lon0"]),
        "outage_start_s": pre_s, "outage_duration_s": outage_duration_s,
        "summary": {
            "mean_error_fused_m": round(float(err_fused.mean()), 1),
            "mean_error_fused_during_outage_m": round(float(err_fused[outage_mask].mean()), 1),
            "max_error_fused_m": round(float(err_fused.max()), 1),
        },
        "rows": rows,
    }

    with open(out_path, "w") as f:
        json.dump(payload, f)
    print(f"wrote {len(rows)} rows to {out_path}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("recording_id", nargs="?", default="S1")
    parser.add_argument("--outage-start", type=float, default=2240.0)
    parser.add_argument("--outage-duration", type=float, default=30.0)
    parser.add_argument("--pre-seconds", type=float, default=15.0)
    parser.add_argument("--post-seconds", type=float, default=15.0)
    parser.add_argument("--alpha-gps", type=float, default=0.9)
    parser.add_argument("--out", default=None)
    args = parser.parse_args()
    out_path = args.out or f"data/{args.recording_id}_track.json"
    main(args.recording_id, args.outage_start, args.outage_duration, args.pre_seconds, args.post_seconds,
         args.alpha_gps, out_path)
