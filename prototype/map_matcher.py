"""P2: simplest useful map matching -- snap each estimated position to its
nearest OSM road segment (not full HMM/Viterbi, per PLAN.md scope).
"""
from pathlib import Path

import numpy as np
import osmnx as ox
from scipy.spatial import cKDTree

from geo import latlon_to_local_xy

CACHE_DIR = Path("data/osm_cache")


def download_road_graph(lat_min, lat_max, lon_min, lon_max, margin_deg=0.01, cache_name=None):
    """Download (or load a cached copy of) the drivable OSM road graph
    covering a recording's bounding box, with a small margin.
    """
    if cache_name:
        CACHE_DIR.mkdir(parents=True, exist_ok=True)
        cache_path = CACHE_DIR / f"{cache_name}.graphml"
        if cache_path.exists():
            return ox.load_graphml(cache_path)

    bbox = (lon_min - margin_deg, lat_min - margin_deg, lon_max + margin_deg, lat_max + margin_deg)
    graph = ox.graph_from_bbox(bbox, network_type="drive")

    if cache_name:
        ox.save_graphml(graph, cache_path)
    return graph


def edges_local_xy(graph, lat0, lon0):
    """Return every graph edge as a local-xy line segment [(x1,y1),(x2,y2)],
    projected with the same equirectangular frame used for the vehicle
    track, so nearest-segment search is a simple 2D geometry problem.
    """
    segments = []
    for u, v, data in graph.edges(data=True):
        if "geometry" in data:
            xs, ys = data["geometry"].xy  # xs=lon, ys=lat
            lon_pts, lat_pts = np.array(xs), np.array(ys)
        else:
            nodes = graph.nodes
            lat_pts = np.array([nodes[u]["y"], nodes[v]["y"]])
            lon_pts = np.array([nodes[u]["x"], nodes[v]["x"]])
        x_pts, y_pts = latlon_to_local_xy(lat_pts, lon_pts, lat0, lon0)
        for i in range(len(x_pts) - 1):
            segments.append(((x_pts[i], y_pts[i]), (x_pts[i + 1], y_pts[i + 1])))
    return segments


def _point_segment_projection(px, py, ax, ay, bx, by):
    """Closest point on segment AB to point P, and the distance to it."""
    abx, aby = bx - ax, by - ay
    ab_len_sq = abx**2 + aby**2
    if ab_len_sq == 0:
        return ax, ay, np.hypot(px - ax, py - ay)
    t = ((px - ax) * abx + (py - ay) * aby) / ab_len_sq
    t = max(0.0, min(1.0, t))
    proj_x, proj_y = ax + t * abx, ay + t * aby
    return proj_x, proj_y, np.hypot(px - proj_x, py - proj_y)


def build_segment_index(segments):
    """KD-tree over every segment's two endpoints, mapping each indexed
    point back to its segment -- lets us shortlist a handful of nearby
    candidate segments per query instead of checking all of them.
    """
    points = np.empty((2 * len(segments), 2))
    seg_ids = np.empty(2 * len(segments), dtype=int)
    for i, (a, b) in enumerate(segments):
        points[2 * i] = a
        points[2 * i + 1] = b
        seg_ids[2 * i] = i
        seg_ids[2 * i + 1] = i
    return cKDTree(points), seg_ids


def snap_point(x, y, segments, tree, seg_ids, k=20, max_dist=50.0):
    """Snap a single (x, y) point to its nearest road segment within
    max_dist metres. Returns (snapped_x, snapped_y, distance) -- the
    original point (unsnapped) if nothing is close enough.
    """
    k = min(k, len(seg_ids))
    _, idxs = tree.query([x, y], k=k)
    best = (x, y, np.inf)
    for sid in set(np.atleast_1d(seg_ids[idxs])):
        (ax, ay), (bx, by) = segments[sid]
        px, py, d = _point_segment_projection(x, y, ax, ay, bx, by)
        if d < best[2]:
            best = (px, py, d)
    if best[2] > max_dist:
        return x, y, best[2]
    return best


def snap_track(x_arr, y_arr, segments, tree, seg_ids, k=20, max_dist=50.0):
    """Snap every (x, y) point to its nearest road segment within
    max_dist metres, using the KD-tree to shortlist candidates. Points
    with no segment close enough are returned unsnapped.
    """
    query_pts = np.column_stack([x_arr, y_arr])
    k = min(k, len(seg_ids))
    _, idxs = tree.query(query_pts, k=k)
    idxs = np.atleast_2d(idxs)

    n = len(x_arr)
    snapped_x = np.empty(n)
    snapped_y = np.empty(n)
    dists = np.empty(n)
    for i in range(n):
        x, y = x_arr[i], y_arr[i]
        best = (x, y, np.inf)
        for sid in set(seg_ids[idxs[i]]):
            (ax, ay), (bx, by) = segments[sid]
            px, py, d = _point_segment_projection(x, y, ax, ay, bx, by)
            if d < best[2]:
                best = (px, py, d)
        if best[2] > max_dist:
            snapped_x[i], snapped_y[i], dists[i] = x, y, best[2]
        else:
            snapped_x[i], snapped_y[i], dists[i] = best
    return snapped_x, snapped_y, dists


if __name__ == "__main__":
    from data_loader import find_recording, load_recording

    smartphone_csv, vehicle_csv = find_recording("S1")
    df = load_recording(smartphone_csv, vehicle_csv)
    graph = download_road_graph(df.lat.min(), df.lat.max(), df.lon.min(), df.lon.max(), cache_name="S1")
    print(f"road graph: {len(graph.nodes)} nodes, {len(graph.edges)} edges")

    lat0, lon0 = df.lat.iloc[0], df.lon.iloc[0]
    segments = edges_local_xy(graph, lat0, lon0)
    print(f"{len(segments)} road segments")

    tree, seg_ids = build_segment_index(segments)
    x_gt, y_gt = latlon_to_local_xy(df.lat, df.lon, lat0, lon0)
    sx, sy, dists = snap_track(x_gt.values, y_gt.values, segments, tree, seg_ids)
    print(f"snap distance for all {len(dists)} ground-truth points: mean {dists.mean():.1f} m, max {dists.max():.1f} m")
