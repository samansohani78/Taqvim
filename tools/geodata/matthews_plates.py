#!/usr/bin/env python3
# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Builds feature/map's tectonic plate boundary asset (T-1301, DT-035) from the EarthByte plate model (CC BY 4.0).

Usage: matthews_plates.py <Matthews_etal_2016_GPC.zip> <retrieved-date> <output.txt>
       matthews_plates.py --check <output.txt>

Source: Matthews, K.J., Maloney, K.T., Zahirovic, S., Williams, S.E., Seton, M., Müller, R.D. (2016), Global plate
boundary evolution and kinematics since the late Paleozoic, https://zenodo.org/records/10526157 (CC BY 4.0). The zip's
present-day static plate polygons (`CorrectedModel/StaticGeometries/StaticPolygons/
PresentDay_StaticPlatePolygons_Matthews.shp` and `.dbf`) are read directly with a minimal shapefile reader; no GPlates
software is used.

Output lines after the header: "P <km²>" (a boundary between polygons of two different plate ids, open) followed by
"lon,lat" pairs in hundredths of a degree, simplified with a Douglas-Peucker tolerance of TOLERANCE degrees. The number
is the spherical area of the smaller of the two plates (all polygons of a plate id), so the app can hide microplate
boundaries at low zoom. Edges between polygons of one plate (for example crust of different ages) are dropped.
"""
import collections
import pathlib
import sys
import zipfile

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import line_layers  # noqa: E402

URL = "https://zenodo.org/records/10526157/files/Matthews_etal_2016_GPC.zip"
MEMBER = "CorrectedModel/StaticGeometries/StaticPolygons/PresentDay_StaticPlatePolygons_Matthews"
TOLERANCE = 0.05


def main(argv):
    if len(argv) == 3 and argv[1] == "--check":
        return line_layers.check_asset(argv[2])
    if len(argv) != 4:
        print(__doc__, file=sys.stderr)
        return 2
    source, retrieved, output = argv[1:]
    with zipfile.ZipFile(source) as archive:
        shapes = line_layers.read_polygon_shapefile(archive.read(MEMBER + ".shp"))
        records = line_layers.read_dbf(archive.read(MEMBER + ".dbf"))
    if len(shapes) != len(records):
        raise ValueError(f"{len(shapes)} shapes but {len(records)} attribute records")
    areas = collections.defaultdict(float)
    for record, rings in zip(records, shapes):
        areas[record["PLATEID1"]] += line_layers.polygon_area_km2(rings)
    groups, single, points = line_layers.pair_segments(
        (record["PLATEID1"], rings) for record, rings in zip(records, shapes)
    )
    body = []
    for (first, second), segments in groups.items():
        smaller = round(min(areas[first], areas[second]))
        body += line_layers.encode_lines(f"P {smaller}", line_layers.chains(segments), points, TOLERANCE)
    header = [
        "# source: Matthews, K.J., Maloney, K.T., Zahirovic, S., Williams, S.E., Seton, M., Muller, R.D. (2016), "
        "Global plate boundary evolution and kinematics since the late Paleozoic; present-day static plate polygons "
        f"({MEMBER}.shp)",
        f"# url: {URL}",
        f"# retrieved: {retrieved}",
        "# license: CC BY 4.0 (https://creativecommons.org/licenses/by/4.0/); simplified for display",
        f"# source-sha256: {line_layers.sha256_file(source)}",
        "# generator: tools/geodata/matthews_plates.py",
        "# format: P = boundary between polygons of different plate ids (PLATEID1), then the smaller plate's spherical "
        f"area in km2; lon,lat pairs in hundredths of a degree; Douglas-Peucker tolerance {TOLERANCE} degree",
    ]
    line_layers.write_asset(output, header, body)
    print(
        f"{output}: {len(body)} lines, {sum(line.count(' ') - 1 for line in body)} points from {len(shapes)} polygons "
        f"and {len(areas)} plate ids; single edges {single}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
