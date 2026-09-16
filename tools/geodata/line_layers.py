# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Shared helpers of the map line-layer generators (T-1301): boundary extraction, geometry, simplification and assets.

Boundaries are the polygon edges shared by two polygons whose keys differ (for example two time-zone bands or two
plate ids). Edges are matched on their endpoints rounded to 1e-5 degree and grouped by the pair of keys they separate;
each group is chained into polylines through vertices with exactly two of its edges, simplified with the
Douglas-Peucker algorithm in degrees, rounded to hundredths of a degree and split where a line would jump across the
antimeridian. The asset format is the one of `natural_earth_outline.py`: header lines starting with "# ", then
"<tag> [fields] lon,lat lon,lat ..." lines.
"""
import collections
import hashlib
import math
import struct

KEY_DIGITS = 5
# Mean Earth radius (IUGG), for polygon areas.
EARTH_RADIUS_KM = 6371.0088


def _key(point):
    return round(point[0], KEY_DIGITS), round(point[1], KEY_DIGITS)


def pair_segments(polygons):
    """Segments shared by exactly two polygons with different keys, grouped by their sorted pair of keys.

    `polygons` is an iterable of (key, rings), rings being lists of (lon, lat); keys must be sortable. Returns a dict
    from (key, key) to that pair's segments (pairs of keyed points), the number of segments owned by one polygon only
    (outer edges, e.g. along the antimeridian) and the original point of every keyed point.
    """
    owners = collections.defaultdict(list)
    points = {}
    for key, rings in polygons:
        for ring in rings:
            for a, b in zip(ring, ring[1:]):
                ka, kb = _key(a), _key(b)
                if ka == kb:
                    continue
                points.setdefault(ka, a)
                points.setdefault(kb, b)
                owners[frozenset((ka, kb))].append(key)
    groups = collections.defaultdict(list)
    single = 0
    for segment, owned in owners.items():
        if len(owned) == 1:
            single += 1
        elif len(owned) == 2 and owned[0] != owned[1]:
            groups[tuple(sorted(owned))].append(tuple(sorted(segment)))
    return dict(sorted(groups.items())), single, points


def chains(segments):
    """Polylines of keyed points through vertices of degree two, in a deterministic order."""
    neighbours = collections.defaultdict(list)
    for a, b in segments:
        neighbours[a].append(b)
        neighbours[b].append(a)
    for node in neighbours:
        neighbours[node].sort()
    used = set()
    lines = []

    def walk(start, following):
        line = [start, following]
        used.add(frozenset((start, following)))
        previous, current = start, following
        while len(neighbours[current]) == 2:
            nxt = neighbours[current][0] if neighbours[current][1] == previous else neighbours[current][1]
            edge = frozenset((current, nxt))
            if edge in used:
                break
            used.add(edge)
            line.append(nxt)
            previous, current = current, nxt
        return line

    for node in sorted(neighbours):
        if len(neighbours[node]) != 2:
            for following in neighbours[node]:
                if frozenset((node, following)) not in used:
                    lines.append(walk(node, following))
    for node in sorted(neighbours):
        for following in neighbours[node]:
            if frozenset((node, following)) not in used:
                lines.append(walk(node, following))
    return lines


def inside(lon, lat, rings):
    """Whether (lon, lat) lies inside a polygon's rings (outer ring and holes) by the even-odd rule, in degrees."""
    crossings = 0
    for ring in rings:
        for (x1, y1), (x2, y2) in zip(ring, ring[1:]):
            if (y1 > lat) != (y2 > lat) and lon < x1 + (lat - y1) * (x2 - x1) / (y2 - y1):
                crossings += 1
    return crossings % 2 == 1


def polygon_area_km2(rings):
    """Area of a polygon's closed rings on the sphere, holes subtracted whatever the ring orientation.

    Each ring's signed area is R²/2 · Σ (λ₂ − λ₁)(2 + sin φ₁ + sin φ₂) over its edges (Chamberlain & Duquette, "Some
    algorithms for polygons on a sphere", JPL Publication 07-3, 2007); outer rings and holes have opposite orientation
    in both GeoJSON and shapefiles, so the absolute value of the sum is the polygon's area.
    """
    total = 0.0
    for ring in rings:
        for (lon1, lat1), (lon2, lat2) in zip(ring, ring[1:]):
            total += math.radians(lon2 - lon1) * (2 + math.sin(math.radians(lat1)) + math.sin(math.radians(lat2)))
    return abs(total) * EARTH_RADIUS_KM**2 / 2


def centroid(ring):
    """The planar (degree) centroid of a closed ring; its first point for a degenerate ring."""
    doubled, cx, cy = 0.0, 0.0, 0.0
    for (x1, y1), (x2, y2) in zip(ring, ring[1:]):
        cross = x1 * y2 - x2 * y1
        doubled += cross
        cx += (x1 + x2) * cross
        cy += (y1 + y2) * cross
    if abs(doubled) < 1e-12:
        return ring[0]
    return cx / (3 * doubled), cy / (3 * doubled)


def simplify(points, tolerance):
    """Douglas-Peucker simplification of (lon, lat) points with a planar tolerance in degrees."""
    if len(points) < 3:
        return list(points)
    keep = [False] * len(points)
    keep[0] = keep[-1] = True
    stack = [(0, len(points) - 1)]
    while stack:
        first, last = stack.pop()
        ax, ay = points[first]
        bx, by = points[last]
        dx, dy = bx - ax, by - ay
        length = (dx * dx + dy * dy) ** 0.5
        farthest, distance = None, tolerance
        for index in range(first + 1, last):
            px, py = points[index]
            if length == 0:
                gap = ((px - ax) ** 2 + (py - ay) ** 2) ** 0.5
            else:
                gap = abs(dy * px - dx * py + bx * ay - by * ax) / length
            if gap > distance:
                farthest, distance = index, gap
        if farthest is not None:
            keep[farthest] = True
            stack.append((first, farthest))
            stack.append((farthest, last))
    return [point for point, kept in zip(points, keep) if kept]


def hundredths(point):
    """A (lon, lat) point as the asset's "lon,lat" pair in hundredths of a degree."""
    return f"{round(point[0] * 100)},{round(point[1] * 100)}"


def encode_lines(tag, lines, points, tolerance):
    """Asset lines for keyed polylines: simplified, rounded to hundredths, split at antimeridian jumps.

    `tag` is written before the pairs and may carry fields, e.g. "Z 3 17".
    """
    out = []
    for line in lines:
        coordinates = simplify([points[key] for key in line], tolerance)
        pairs = []
        previous_lon = None
        for lon, lat in coordinates:
            if previous_lon is not None and abs(lon - previous_lon) > 180:
                if len(pairs) >= 2:
                    out.append(f"{tag} " + " ".join(pairs))
                pairs = []
            pair = hundredths((lon, lat))
            if not pairs or pairs[-1] != pair:
                pairs.append(pair)
            previous_lon = lon
        if len(pairs) >= 2:
            out.append(f"{tag} " + " ".join(pairs))
    return out


def sha256_file(path):
    with open(path, "rb") as handle:
        return hashlib.sha256(handle.read()).hexdigest()


def body_hash(lines):
    return hashlib.sha256("\n".join(lines).encode("utf-8")).hexdigest()


def write_asset(output, header, body):
    with open(output, "w", encoding="utf-8", newline="\n") as handle:
        handle.write("\n".join(header + [f"# body-sha256: {body_hash(body)}"] + body) + "\n")


def check_asset(output):
    """0 when the asset's body matches the `body-sha256` in its header, else 1."""
    with open(output, encoding="utf-8") as handle:
        lines = handle.read().splitlines()
    declared = next((line[len("# body-sha256: "):] for line in lines if line.startswith("# body-sha256: ")), None)
    body = [line for line in lines if not line.startswith("#")]
    return 0 if declared == body_hash(body) else 1


def read_polygon_shapefile(data):
    """Rings of every record of an ESRI polygon shapefile's bytes (shape type 5); null shapes give no rings."""
    if struct.unpack("<i", data[32:36])[0] != 5:
        raise ValueError("not a polygon shapefile")
    records, offset = [], 100
    while offset < len(data):
        _, length = struct.unpack(">ii", data[offset:offset + 8])
        record = data[offset + 8:offset + 8 + length * 2]
        offset += 8 + length * 2
        if struct.unpack("<i", record[:4])[0] == 0:
            records.append([])
            continue
        part_count, point_count = struct.unpack("<ii", record[36:44])
        starts = list(struct.unpack(f"<{part_count}i", record[44:44 + 4 * part_count]))
        values = struct.unpack(f"<{2 * point_count}d", record[44 + 4 * part_count:44 + 4 * part_count + 16 * point_count])
        coordinates = [(values[2 * i], values[2 * i + 1]) for i in range(point_count)]
        ends = starts[1:] + [point_count]
        records.append([coordinates[start:end] for start, end in zip(starts, ends)])
    return records


def read_dbf(data):
    """Records of a dBase III table's bytes as dicts of stripped strings (Latin-1)."""
    count = struct.unpack("<I", data[4:8])[0]
    header_length, record_length = struct.unpack("<HH", data[8:12])
    fields, offset = [], 32
    while data[offset] != 0x0D:
        fields.append((data[offset:offset + 11].split(b"\0")[0].decode("ascii"), data[offset + 16]))
        offset += 32
    records = []
    for index in range(count):
        raw = data[header_length + index * record_length + 1:header_length + (index + 1) * record_length]
        record, position = {}, 0
        for name, length in fields:
            record[name] = raw[position:position + length].decode("latin-1").strip()
            position += length
        records.append(record)
    return records
