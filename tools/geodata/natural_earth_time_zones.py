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

Mixed bands (review I09): a 2012 band may now hold places whose offsets have diverged. The other zones of the
catalog cities inside a band whose total UTC offset differs from the representative zone's at any of the REFERENCE
instants (mid-January and mid-July, so daylight saving counts) are listed with the band, most populous first. A city
counts only when the tz database's reference location of its zone (zone1970.tab / zone.tab) is within
MAX_ZONE_DISTANCE_KM of it, which drops wrong zone ids in the city catalog. The app compares the listed zones with the
representative zone at the shown moment and marks the band's label when they differ.

Output lines after the header:
- "T <zone|-> <2012 offset minutes> <label lon,lat> <area km²> [<zone>,<zone>…]", one per band in index order; the
  label point is the band's centroid, or its representative city when the centroid falls outside the band; the
  optional last field lists the band's other zones whose offsets differ from the representative zone's;
- "Z <band> <band> lon,lat ..." (the boundary between two bands, open), simplified with a Douglas-Peucker tolerance of
  TOLERANCE degrees.
Coordinates are in hundredths of a degree; on a boundary line the first pair is absolute and every later
pair is relative to the point before it.
"""
import datetime
import json
import math
import pathlib
import sys
import zoneinfo

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import line_layers  # noqa: E402

URL = "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/{commit}/geojson/ne_10m_time_zones.geojson"
TOLERANCE = 0.05
MAX_ZONE_DISTANCE_KM = 1500
EARTH_RADIUS_KM = 6371.0088
REFERENCE = (
    datetime.datetime(2026, 1, 15, 12, tzinfo=datetime.timezone.utc),
    datetime.datetime(2026, 7, 15, 12, tzinfo=datetime.timezone.utc),
)


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

    cities.tsv is column-major (T-1800): after the header, one line per column of "# columns:" holding that column's
    value for every place. Backward names (links such as Asia/Rangoon) are replaced by their canonical zone.
    """
    available, links = zoneinfo.available_timezones(), tzdata_links()
    names, columns = [], []
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            line = line.rstrip("\n")
            if line.startswith("# columns: "):
                names = line.removeprefix("# columns: ").split(",")
            elif not line.startswith("#") and line:
                columns.append(line.split("\t"))
    if len(columns) != len(names):
        raise ValueError(f"{path}: {len(columns)} column lines for {len(names)} columns")
    table = dict(zip(names, columns))
    cities = []
    for place in range(len(table["neId"])):
        zone = links.get(table["timeZone"][place], table["timeZone"][place])
        if zone in available:
            cities.append(
                (
                    float(table["longitude"][place]),
                    float(table["latitude"][place]),
                    zone,
                    int(table["population"][place] or 0),
                )
            )
    return cities


def band_cities(parts, cities):
    """The catalog cities (lon, lat, zone, population) inside each band, most populous first."""
    boxes = []
    for _, rings in parts:
        xs = [x for ring in rings for x, _ in ring]
        ys = [y for ring in rings for _, y in ring]
        boxes.append((min(xs), max(xs), min(ys), max(ys)))
    inside = [[] for _ in parts]
    for city in cities:
        lon, lat = city[0], city[1]
        for index, (_, rings) in enumerate(parts):
            x0, x1, y0, y1 = boxes[index]
            if x0 <= lon <= x1 and y0 <= lat <= y1 and line_layers.inside(lon, lat, rings):
                inside[index].append(city)
                break
    return [sorted(found, key=lambda city: -city[3]) for found in inside]


def iso6709(text):
    """(lat, lon) in degrees of an ISO 6709 "±DDMM[SS]±DDDMM[SS]" string from the tz database tables."""
    split = max(text.rfind("+"), text.rfind("-"))
    values = []
    for part, width in ((text[:split], 2), (text[split:], 3)):
        sign = -1 if part[0] == "-" else 1
        digits = part[1:]
        degrees, minutes, seconds = digits[:width], digits[width:width + 2], digits[width + 2:] or "0"
        values.append(sign * (int(degrees) + int(minutes) / 60 + int(seconds) / 3600))
    return values[0], values[1]


def zone_locations():
    """Zone → (lat, lon) of its reference location from the tz database's zone.tab and zone1970.tab."""
    locations = {}
    for candidate in zoneinfo.TZPATH:
        for name in ("zone.tab", "zone1970.tab"):
            path = pathlib.Path(candidate) / name
            if path.exists():
                for line in path.read_text(encoding="utf-8").splitlines():
                    fields = line.split("\t")
                    if not line.startswith("#") and len(fields) >= 3:
                        locations[fields[2]] = iso6709(fields[1])
        if locations:
            break
    return locations


def distance_km(lat1, lon1, lat2, lon2):
    """Great-circle distance by the haversine formula."""
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp, dl = p2 - p1, math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * EARTH_RADIUS_KM * math.asin(math.sqrt(a))


def plausible(city, locations):
    """Whether [city]'s zone has its tz reference location within MAX_ZONE_DISTANCE_KM of the city."""
    lon, lat, zone, _ = city
    location = locations.get(zone)
    return location is not None and distance_km(lat, lon, location[0], location[1]) <= MAX_ZONE_DISTANCE_KM


def offsets(zone):
    """[zone]'s total UTC offsets in minutes at the REFERENCE instants."""
    tz = zoneinfo.ZoneInfo(zone)
    return tuple(round(moment.astimezone(tz).utcoffset().total_seconds() / 60) for moment in REFERENCE)


def conflicting_zones(representative, cities, locations):
    """The distinct zones of plausible [cities] (most populous first) whose offsets differ from [representative]'s."""
    if representative is None:
        return []
    own = offsets(representative)
    zones = []
    for city in cities:
        zone = city[2]
        if zone != representative and zone not in zones and plausible(city, locations) and offsets(zone) != own:
            zones.append(zone)
    return zones


def band_lines(features, parts, cities):
    """The "T" lines and the counts of bands with an own, an inherited and no representative city, and mixed bands."""
    inside = band_cities(parts, cities)
    locations = zone_locations()
    by_feature = {}
    for (feature, _), found in zip(parts, inside):
        if found and (feature not in by_feature or found[0][3] > by_feature[feature][3]):
            by_feature[feature] = found[0]
    lines, counts = [], [0, 0, 0, 0]
    for (feature, rings), found in zip(parts, inside):
        city = found[0] if found else None
        chosen = city or by_feature.get(feature)
        counts[0 if city else 1 if chosen else 2] += 1
        label = line_layers.centroid(rings[0])
        if not line_layers.inside(label[0], label[1], rings):
            label = (chosen[0], chosen[1]) if chosen else rings[0][0]
        offset = round(float(features[feature]["properties"]["zone"]) * 60)
        # Rocks and reefs round to nothing; every band keeps at least 1 km² so labels can be ordered by size.
        area = max(1, round(line_layers.polygon_area_km2(rings)))
        zone = chosen[2] if chosen else "-"
        others = conflicting_zones(chosen[2] if chosen else None, found, locations)
        counts[3] += 1 if others else 0
        suffix = " " + ",".join(others) if others else ""
        lines.append(f"T {zone} {offset} {line_layers.hundredths(label)} {area}{suffix}")
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
    band_body, (own, inherited, none, mixed) = band_lines(features, parts, read_cities(cities_path))
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
        "# format: T = band (representative IANA zone or -, 2012 offset in minutes, label lon,lat, area km2, optional "
        "comma-separated other zones with another offset), one per "
        f"band in index order; Z = boundary between two bands (their indices); lon,lat pairs in hundredths of a "
        "degree, a band label absolute and, on a boundary, the first pair absolute and every later one relative to "
        f"the point before it; Douglas-Peucker tolerance {TOLERANCE} degree",
        f"# bands: {len(parts)} ({own} with a catalog city, {inherited} with their feature's city, {none} without); "
        f"tz database {tz_version()}",
        f"# mixed bands: {mixed} (reference instants "
        + ", ".join(moment.strftime("%Y-%m-%dT%H:%MZ") for moment in REFERENCE)
        + f"; other zones only when their zone.tab location is within {MAX_ZONE_DISTANCE_KM} km of the city)",
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
