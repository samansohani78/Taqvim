#!/usr/bin/env python3
# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Builds feature/map's time-zone boundary asset (T-1301, DT-035) from Natural Earth 1:10m Time Zones (public domain).

Usage: natural_earth_time_zones.py <ne_10m_time_zones.geojson> <commit> <retrieved-date> <cities.tsv> <output.txt>
       natural_earth_time_zones.py --check <output.txt>

Output lines after the header: "Z" (a boundary between two UTC-offset bands, open) followed by "lon,lat" pairs in
hundredths of a degree. Only edges shared by two bands with different `zone` values are kept (see line_layers.py);
lines are simplified with a Douglas-Peucker tolerance of TOLERANCE degrees. The data dates from 2012 (the CIA World
Factbook map), so every catalog city (T-603 cities.tsv, with its IANA zone) inside a band is compared with the band's
offset using this machine's tz database; bands whose cities now keep another standard offset are printed and
summarised in the header (zones with at least SYSTEMATIC_CITIES such cities in one band). Geometry is never changed.
"""
import datetime
import json
import pathlib
import sys
import zoneinfo

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import line_layers  # noqa: E402

URL = "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/{commit}/geojson/ne_10m_time_zones.geojson"
TOLERANCE = 0.05
CHECK_INSTANT = datetime.datetime(2026, 1, 15, 12, tzinfo=datetime.timezone.utc)
# Fewer mismatching cities than this are usually a city next to a band edge or a catalog city with a wrong zone id.
SYSTEMATIC_CITIES = 3


def polygons(features):
    for feature in features:
        geometry = feature["geometry"]
        parts = geometry["coordinates"] if geometry["type"] == "MultiPolygon" else [geometry["coordinates"]]
        for rings in parts:
            yield feature, rings


def inside(lon, lat, rings):
    """Even-odd rule over the outer ring and its holes."""
    crossings = 0
    for ring in rings:
        for (x1, y1), (x2, y2) in zip(ring, ring[1:]):
            if (y1 > lat) != (y2 > lat) and lon < x1 + (lat - y1) * (x2 - x1) / (y2 - y1):
                crossings += 1
    return crossings % 2 == 1


def tzdata_links():
    """Alias → target of the tz database's links ("L target alias" lines of tzdata.zi), for backward names."""
    for candidate in zoneinfo.TZPATH:
        path = pathlib.Path(candidate) / "tzdata.zi"
        if path.exists():
            lines = path.read_text(encoding="utf-8").splitlines()
            return {parts[2]: parts[1] for parts in (line.split() for line in lines) if parts and parts[0] == "L"}
    return {}


def standard_offset_hours(zone_name, available, links):
    name = zone_name if zone_name in available else links.get(zone_name, zone_name)
    moment = CHECK_INSTANT.astimezone(zoneinfo.ZoneInfo(name))
    return (moment.utcoffset() - moment.dst()).total_seconds() / 3600


def read_cities(path):
    with open(path, encoding="utf-8") as handle:
        rows = [line.rstrip("\n").split("\t") for line in handle if not line.startswith("#")]
    return [(float(row[4]), float(row[3]), row[5], row[7]) for row in rows if row[5]]


def offset_check(features, cities):
    """(band zone, band places, IANA zone, its standard offset, city count, example) for mismatching cities."""
    bands = []
    for feature, rings in polygons(features):
        xs = [x for ring in rings for x, _ in ring]
        ys = [y for ring in rings for _, y in ring]
        bands.append((feature, rings, (min(xs), max(xs), min(ys), max(ys))))
    mismatches = {}
    available, links = zoneinfo.available_timezones(), tzdata_links()
    for lon, lat, zone_name, name in cities:
        actual = standard_offset_hours(zone_name, available, links)
        for feature, rings, (x0, x1, y0, y1) in bands:
            if x0 <= lon <= x1 and y0 <= lat <= y1 and inside(lon, lat, rings):
                band = float(feature["properties"]["zone"])
                if abs(band - actual) > 1e-9:
                    key = (band, feature["properties"]["places"] or "", zone_name, actual)
                    count, example = mismatches.get(key, (0, name))
                    mismatches[key] = (count + 1, example)
                break
    return sorted((key + value) for key, value in mismatches.items())


def main(argv):
    if len(argv) == 3 and argv[1] == "--check":
        return line_layers.check_asset(argv[2])
    if len(argv) != 6:
        print(__doc__, file=sys.stderr)
        return 2
    source, commit, retrieved, cities_path, output = argv[1:]
    with open(source, encoding="utf-8") as handle:
        features = json.load(handle)["features"]
    segments, single, points = line_layers.boundary_segments(
        (feature["properties"]["zone"], rings) for feature, rings in polygons(features)
    )
    body = line_layers.encode_lines("Z", line_layers.chains(segments), points, TOLERANCE)
    mismatches = offset_check(features, read_cities(cities_path))
    changed_zones = sorted({row[2] for row in mismatches if row[4] >= SYSTEMATIC_CITIES})
    header = [
        "# source: Natural Earth 1:10m Cultural Vectors, Time Zones (ne_10m_time_zones.geojson; content of 2012, "
        "from the CIA World Factbook time zone map)",
        "# url: " + URL.format(commit=commit),
        f"# retrieved: {retrieved}",
        "# license: public domain (https://www.naturalearthdata.com/about/terms-of-use/)",
        f"# source-sha256: {line_layers.sha256_file(source)}",
        "# generator: tools/geodata/natural_earth_time_zones.py",
        f"# format: Z = boundary between UTC-offset bands; lon,lat pairs in hundredths of a degree; Douglas-Peucker "
        f"tolerance {TOLERANCE} degree",
        f"# offset-check: tz database {tz_version()} at {CHECK_INSTANT.date()}: catalog cities of these IANA zones "
        f"(at least {SYSTEMATIC_CITIES} per band) keep another standard offset than their 2012 band: "
        + ", ".join(changed_zones),
    ]
    line_layers.write_asset(output, header, body)
    print(f"{output}: {len(body)} lines, {sum(line.count(' ') for line in body)} points; single edges {single}")
    for band, places, zone_name, actual, count, example in mismatches:
        print(f"band {band:+g} ({places[:60]}): {zone_name} now {actual:+g}, {count} cities, e.g. {example}")
    return 0


def tz_version():
    for candidate in zoneinfo.TZPATH:
        path = pathlib.Path(candidate) / "tzdata.zi"
        if path.exists():
            return path.read_text(encoding="utf-8").splitlines()[0].removeprefix("# version ").strip()
    return "unknown"


if __name__ == "__main__":
    sys.exit(main(sys.argv))
