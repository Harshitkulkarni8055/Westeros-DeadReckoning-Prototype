"""Small shared geometry helpers for the dead-reckoning prototype."""
import numpy as np

EARTH_RADIUS_M = 6371000.0


def latlon_to_local_xy(lat, lon, lat0=None, lon0=None):
    """Small-area equirectangular projection: east/north metres relative to
    (lat0, lon0), defaulting to the first point. Good enough for a single
    recording, not for large-area mapping.
    """
    if lat0 is None:
        lat0 = lat.iloc[0] if hasattr(lat, "iloc") else lat[0]
    if lon0 is None:
        lon0 = lon.iloc[0] if hasattr(lon, "iloc") else lon[0]
    lat0_rad = np.radians(lat0)
    lon0_rad = np.radians(lon0)
    x = (np.radians(lon) - lon0_rad) * np.cos(lat0_rad) * EARTH_RADIUS_M
    y = (np.radians(lat) - lat0_rad) * EARTH_RADIUS_M
    return x, y


def local_xy_to_latlon(x, y, lat0, lon0):
    """Inverse of latlon_to_local_xy."""
    lat0_rad = np.radians(lat0)
    lon0_rad = np.radians(lon0)
    lat = np.degrees(y / EARTH_RADIUS_M + lat0_rad)
    lon = np.degrees(x / (EARTH_RADIUS_M * np.cos(lat0_rad)) + lon0_rad)
    return lat, lon


def wrap_angle(angle_rad):
    """Wrap angle(s) to (-pi, pi]."""
    return (angle_rad + np.pi) % (2 * np.pi) - np.pi
