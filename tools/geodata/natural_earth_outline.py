#!/usr/bin/env python3
# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Builds feature/map's world outline asset (T-1301) from Natural Earth 1:110m vectors (public domain).

Usage: natural_earth_outline.py <ne_110m_land.geojson> <ne_110m_admin_0_boundary_lines_land.geojson>
                                <commit> <retrieved-date> <output.txt>
       natural_earth_outline.py --check <output.txt>

Output lines after the header: "L" (a land ring, closed) or "B" (a country boundary line, open) followed by
space-separated "lon,lat" pairs in hundredths of a degree (integers), the first absolute and every later one relative
to the point before it (T-1800). Coordinates are rounded, consecutive duplicates are dropped and parts with fewer than
two points are skipped; nothing is added. The header records both source files'
SHA-256 and the SHA-256 of the body (every line after the header joined with "\n"), which the module's test checks.
"""
import hashlib
import json
import sys

import line_layers

URL = "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/{commit}/geojson/{name}"


def parts(geometry):
    kind, coordinates = geometry["type"], geometry["coordinates"]
    if kind == "Polygon":
        return [ring for ring in coordinates]
    if kind == "MultiPolygon":
        return [ring for polygon in coordinates for ring in polygon]
    if kind == "LineString":
        return [coordinates]
    if kind == "MultiLineString":
        return list(coordinates)
    raise ValueError(f"unexpected geometry {kind}")


def encode(tag, points):
    pairs = []
    for lon, lat in ((p[0], p[1]) for p in points):
        pair = f"{round(lon * 100)},{round(lat * 100)}"
        if not pairs or pairs[-1] != pair:
            pairs.append(pair)
    return f"{tag} " + " ".join(line_layers.delta_pairs(pairs)) if len(pairs) >= 2 else None


def body_lines(path, tag):
    with open(path, encoding="utf-8") as handle:
        features = json.load(handle)["features"]
    lines = (encode(tag, part) for feature in features for part in parts(feature["geometry"]))
    return [line for line in lines if line]


def sha256_file(path):
    with open(path, "rb") as handle:
        return hashlib.sha256(handle.read()).hexdigest()


def body_hash(lines):
    return hashlib.sha256("\n".join(lines).encode("utf-8")).hexdigest()


def check(output):
    with open(output, encoding="utf-8") as handle:
        lines = handle.read().splitlines()
    header = {line[2:].split(": ", 1)[0]: line[2:].split(": ", 1)[1] for line in lines if line.startswith("# ")}
    body = [line for line in lines if not line.startswith("#")]
    if header.get("body-sha256") != body_hash(body):
        print(f"{output}: body hash does not match its header", file=sys.stderr)
        return 1
    return 0


def main(argv):
    if len(argv) == 3 and argv[1] == "--check":
        return check(argv[2])
    if len(argv) != 6:
        print(__doc__, file=sys.stderr)
        return 2
    land, boundaries, commit, retrieved, output = argv[1:]
    body = body_lines(land, "L") + body_lines(boundaries, "B")
    header = [
        "# source: Natural Earth 1:110m Physical Vectors, Land (ne_110m_land.geojson); Cultural Vectors, Admin 0 "
        "boundary lines (ne_110m_admin_0_boundary_lines_land.geojson)",
        "# url-land: " + URL.format(commit=commit, name="ne_110m_land.geojson"),
        "# url-boundaries: " + URL.format(commit=commit, name="ne_110m_admin_0_boundary_lines_land.geojson"),
        f"# retrieved: {retrieved}",
        "# license: public domain (https://www.naturalearthdata.com/about/terms-of-use/)",
        f"# source-sha256-land: {sha256_file(land)}",
        f"# source-sha256-boundaries: {sha256_file(boundaries)}",
        "# format: L = land ring, B = boundary line; lon,lat pairs in hundredths of a degree, the first of a "
        "line absolute and every later one relative to the point before it",
        f"# body-sha256: {body_hash(body)}",
    ]
    with open(output, "w", encoding="utf-8", newline="\n") as handle:
        handle.write("\n".join(header + body) + "\n")
    print(f"{output}: {sum(1 for l in body if l[0] == 'L')} land rings, {sum(1 for l in body if l[0] == 'B')} lines")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
