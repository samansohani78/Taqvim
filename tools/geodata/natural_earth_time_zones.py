#!/usr/bin/env python3
# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Builds feature/map's time-zone band asset (T-1301, DT-035) from Natural Earth 1:10m Time Zones (public domain).

Usage: natural_earth_time_zones.py <ne_10m_time_zones.geojson> <commit> <retrieved-date> <cities.tsv> <output.txt>
       natural_earth_time_zones.py --check <output.txt>

Only the band geometry is used for what the map shows: the data dates from 2012 (the CIA World Factbook map), so the
app computes every band's offset itself from the device's tz rules. Each polygon of the source is one band. Its
representative IANA zone is the zone of the most populous catalog city (T-603 cities.tsv) inside the polygon; a polygon
with no catalog city takes the most populous city of its feature (other polygons of the same 2012 region); a band with
neither keeps no zone and is recorded with its 2012 offset only (the app then draws its edges only where the 2012
offsets of both neighbours differ). Zones unknown to this machine's tz database are skipped.

Output lines after the header:
- "T <zone|-> <2012 offset minutes> <label lon,lat> <area km²>", one per band in index order; the label point is the
  band's centroid, or its representative city when the centroid falls outside the band;
- "Z <band> <band> lon,lat ..." (the boundary between two bands, open), simplified with a Douglas-Peucker tolerance of
  TOLERANCE degrees.
Coordinates are in hundredths of a degree.
"""
import json
import pathlib
import sys
import zoneinfo

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import line_layers  # noqa: E402

URL = "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/{commit}/geojson/ne_10m_time_zones.geojson"
TOLERANCE = 0.05


def bands(features):
    """(feature index, rings) of every polygon, in source order."""
    for index, feature in enumerate(features):
        geometry = feature["geometry"]
        parts = geometry["coordinates"] if geometry["type"] == "MultiPolygon" else [geometry["coordinates"]]
        for rings in parts:
            yield index, rings


def tzdata_links():
    """Alias → target of the tz database's links ("L target alias" lines of tzdata.zi), for backward names."""
    for candidate in zoneinfo.TZPATH:
        path = pathlib.Path(candidate) / "tzdata.zi"
        if path.exists():
            lines = path.read_text(encoding="utf-8").splitlines()
            return {parts[2]: parts[1] for parts in (line.split() for line in lines) if parts and parts[0] == "L"}
    return {}


def read_cities(path):
    """(lon, lat, zone, population) of catalog cities with a zone known to this machine's tz database.

    Backward names (links such as Asia/Rangoon) are replaced by their canonical zone.
    """
    available, links = zoneinfo.available_timezones(), tzdata_links()
    with open(path, encoding="utf-8") as handle:
        rows = [line.rstrip("\n").split("\t") for line in handle if not line.startswith("#")]
    cities = []
    for row in rows:
        zone = links.get(row[5], row[5])
        if zone in available:
            cities.append((float(row[4]), float(row[3]), zone, int(row[6] or 0)))
    return cities


def representative_cities(parts, cities):
    """The most populous catalog city (lon, lat, zone, population) inside each band, or None."""
    boxes = []
    for _, rings in parts:
        xs = [x for ring in rings for x, _ in ring]
        ys = [y for ring in rings for _, y in ring]
        boxes.append((min(xs), max(xs), min(ys), max(ys)))
    best = [None] * len(parts)
    for city in cities:
        lon, lat, _, population = city
        for index, (_, rings) in enumerate(parts):
            x0, x1, y0, y1 = boxes[index]
            if x0 <= lon <= x1 and y0 <= lat <= y1 and line_layers.inside(lon, lat, rings):
                if best[index] is None or population > best[index][3]:
                    best[index] = city
                break
    return best


def band_lines(features, parts, cities):
    """The "T" lines and the counts of bands with an own, an inherited and no representative city."""
    own = representative_cities(parts, cities)
    by_feature = {}
    for (feature, _), city in zip(parts, own):
        if city is not None and (feature not in by_feature or city[3] > by_feature[feature][3]):
            by_feature[feature] = city
    lines, counts = [], [0, 0, 0]
    for (feature, rings), city in zip(parts, own):
        chosen = city or by_feature.get(feature)
        counts[0 if city else 1 if chosen else 2] += 1
        label = line_layers.centroid(rings[0])
        if not line_layers.inside(label[0], label[1], rings):
            label = (chosen[0], chosen[1]) if chosen else rings[0][0]
        offset = round(float(features[feature]["properties"]["zone"]) * 60)
        # Rocks and reefs round to nothing; every band keeps at least 1 km² so labels can be ordered by size.
        area = max(1, round(line_layers.polygon_area_km2(rings)))
        zone = chosen[2] if chosen else "-"
        lines.append(f"T {zone} {offset} {line_layers.hundredths(label)} {area}")
    return lines, counts


def main(argv):
    if len(argv) == 3 and argv[1] == "--check":
        return line_layers.check_asset(argv[2])
    if len(argv) != 6:
        print(__doc__, file=sys.stderr)
        return 2
    source, commit, retrieved, cities_path, output = argv[1:]
    with open(source, encoding="utf-8") as handle:
        features = json.load(handle)["features"]
    parts = list(bands(features))
    band_body, (own, inherited, none) = band_lines(features, parts, read_cities(cities_path))
    groups, single, points = line_layers.pair_segments(
        (index, rings) for index, (_, rings) in enumerate(parts)
    )
    edges = []
    for (first, second), segments in groups.items():
        edges += line_layers.encode_lines(f"Z {first} {second}", line_layers.chains(segments), points, TOLERANCE)
    header = [
        "# source: Natural Earth 1:10m Cultural Vectors, Time Zones (ne_10m_time_zones.geojson; content of 2012, "
        "from the CIA World Factbook time zone map)",
        "# url: " + URL.format(commit=commit),
        f"# retrieved: {retrieved}",
        "# license: public domain (https://www.naturalearthdata.com/about/terms-of-use/)",
        f"# source-sha256: {line_layers.sha256_file(source)}",
        f"# cities-sha256: {line_layers.sha256_file(cities_path)}",
        "# generator: tools/geodata/natural_earth_time_zones.py",
        "# format: T = band (representative IANA zone or -, 2012 offset in minutes, label lon,lat, area km2), one per "
        f"band in index order; Z = boundary between two bands (their indices); lon,lat pairs in hundredths of a "
        f"degree; Douglas-Peucker tolerance {TOLERANCE} degree",
        f"# bands: {len(parts)} ({own} with a catalog city, {inherited} with their feature's city, {none} without); "
        f"tz database {tz_version()}",
    ]
    line_layers.write_asset(output, header, band_body + edges)
    print(f"{output}: {len(parts)} bands ({own} own city, {inherited} inherited, {none} none), {len(edges)} edges "
          f"between {len(groups)} band pairs; single edges {single}")
    return 0


def tz_version():
    for candidate in zoneinfo.TZPATH:
        path = pathlib.Path(candidate) / "tzdata.zi"
        if path.exists():
            return path.read_text(encoding="utf-8").splitlines()[0].removeprefix("# version ").strip()
    return "unknown"


if __name__ == "__main__":
    sys.exit(main(sys.argv))
