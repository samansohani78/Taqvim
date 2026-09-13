#!/usr/bin/env python3
# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Builds data/location's cities.tsv (T-603) from Natural Earth "Populated Places" (public domain).

Usage: natural_earth_cities.py <ne_10m_populated_places.geojson> <source-url> <retrieved-date> <output.tsv>

One row per populated place, sorted by Natural Earth id. Coordinates come from the point geometry (the LATITUDE /
LONGITUDE attributes disagree with it for some places) rounded to 5 decimals. Names are copied as published:
English is always present; a localized column is empty when the source's name equals the English one. The source's
-99 sentinels (country code, population) and missing time zones become empty fields. Nothing is added or translated.
"""
import hashlib
import json
import sys

LANGUAGES = ["ar", "bn", "de", "es", "fa", "fr", "hi", "id", "ja", "ru", "tr", "ur", "zh"]
# "neId" (Natural Earth id) rather than "id", which is also the language code of Indonesian.
COLUMNS = ["neId", "country", "region", "latitude", "longitude", "timeZone", "population", "en"] + LANGUAGES


def clean(value):
    text = "" if value is None else str(value).strip()
    if any(c in text for c in "\t\r\n"):
        raise ValueError(f"control character in {text!r}")
    return "" if text == "-99" else text


def row(feature):
    p = feature["properties"]
    longitude, latitude = feature["geometry"]["coordinates"][:2]
    english = clean(p["NAME_EN"]) or clean(p["NAME"])
    names = [clean(p[f"NAME_{code.upper()}"]) for code in LANGUAGES]
    population = p["POP_MAX"] if isinstance(p["POP_MAX"], int) and p["POP_MAX"] >= 0 else ""
    fields = [
        str(p["NE_ID"]),
        clean(p["ISO_A2"]),
        clean(p["ADM1NAME"]),
        f"{latitude:.5f}",
        f"{longitude:.5f}",
        clean(p["TIMEZONE"]),
        str(population),
        english,
    ] + ["" if name == english else name for name in names]
    return "\t".join(fields)


def main(source, url, retrieved, output):
    data = open(source, "rb").read()
    features = json.loads(data)["features"]
    rows = [row(f) for f in sorted(features, key=lambda f: f["properties"]["NE_ID"])]
    header = [
        "# source: Natural Earth 1:10m Cultural Vectors, Populated Places (ne_10m_populated_places)",
        f"# url: {url}",
        f"# sha256: {hashlib.sha256(data).hexdigest()}",
        f"# retrieved: {retrieved}",
        "# licence: public domain, https://www.naturalearthdata.com/about/terms-of-use/",
        "# generator: tools/geodata/natural_earth_cities.py",
        "# columns: " + ",".join(COLUMNS),
    ]
    with open(output, "w", encoding="utf-8", newline="\n") as out:
        out.write("\n".join(header + rows) + "\n")
    print(f"{len(rows)} places written to {output}")


if __name__ == "__main__":
    if len(sys.argv) != 5:
        sys.exit(__doc__)
    main(*sys.argv[1:])
