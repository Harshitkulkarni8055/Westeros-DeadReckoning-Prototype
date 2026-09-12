"""P3: simulate a GPS outage and blend GPS/dead-reckoning with a simple
fixed-weight rule (not the proposal's learned fusion network, per scope).

During the outage, position comes entirely from map-matched dead
reckoning. Once GPS returns, the fused position snaps back to a
GPS-weighted blend -- the "seamless switching" the proposal describes,
simplified to a fixed alpha instead of a learned one.
"""
import numpy as np

from map_matcher import snap_point


def run_fused_pipeline(demo, heading, pred_speed, gps_x, gps_y, gt_x, gt_y,
                        gps_available, segments, tree, seg_ids,
                        alpha_gps=0.9, max_snap_dist=60.0):
    """Step through one recording window, fusing GPS with map-matched dead
    reckoning, and separately track what pure open-loop dead reckoning
    (no map correction, no GPS re-anchoring) would have done from the
    moment the outage starts -- for the "raw drift" comparison line.

    Returns a dict of x/y arrays: fused, mapmatched, raw_dr (NaN before
    the outage starts, since that trace only exists to show what
    unconstrained drift looks like during/after the blackout).
    """
    n = len(demo)
    dt = demo.dt.values

    fused_x = np.empty(n)
    fused_y = np.empty(n)
    mm_x = np.empty(n)
    mm_y = np.empty(n)
    raw_x = np.full(n, np.nan)
    raw_y = np.full(n, np.nan)

    fused_x[0], fused_y[0] = gps_x[0], gps_y[0]
    mm_x[0], mm_y[0] = fused_x[0], fused_y[0]
    raw_started = False

    for i in range(1, n):
        distance = pred_speed[i] * dt[i]
        dx = distance * np.sin(heading[i])
        dy = distance * np.cos(heading[i])

        pred_x, pred_y = fused_x[i - 1] + dx, fused_y[i - 1] + dy

        if gps_available[i]:
            mm_x[i], mm_y[i] = pred_x, pred_y
            fused_x[i] = alpha_gps * gps_x[i] + (1 - alpha_gps) * pred_x
            fused_y[i] = alpha_gps * gps_y[i] + (1 - alpha_gps) * pred_y
        else:
            sx, sy, _ = snap_point(pred_x, pred_y, segments, tree, seg_ids, max_dist=max_snap_dist)
            mm_x[i], mm_y[i] = sx, sy
            fused_x[i], fused_y[i] = sx, sy

        if not gps_available[i] and not raw_started:
            raw_started = True
            raw_x[i - 1], raw_y[i - 1] = fused_x[i - 1], fused_y[i - 1]
        if raw_started:
            raw_x[i] = raw_x[i - 1] + dx
            raw_y[i] = raw_y[i - 1] + dy

    return {
        "fused_x": fused_x, "fused_y": fused_y,
        "mapmatched_x": mm_x, "mapmatched_y": mm_y,
        "raw_dr_x": raw_x, "raw_dr_y": raw_y,
    }
