#!/usr/bin/env python3
# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""T-1504: builds the in-app open-source license catalog from the license gate report.

Reads build/reports/licenses/license-report.json (written by `./gradlew licenseCheck`, T-001), keeps the RUNTIME
dependencies and writes feature/about/src/main/assets/licenses/third-party.json. License texts are verbatim files in
feature/about/src/main/assets/licenses/texts/<SPDX id>.txt; a license without a text file is listed with its URL only.

    python3 tools/licenses/about_licenses.py          # regenerate the asset
    python3 tools/licenses/about_licenses.py --check  # exit 1 when the asset is stale
"""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
REPORT = ROOT / "build/reports/licenses/license-report.json"
ASSETS = ROOT / "feature/about/src/main/assets/licenses"
CATALOG = ASSETS / "third-party.json"


def build_catalog(report):
    licenses = {}
    components = []
    for dependency in sorted(report, key=lambda item: item["coordinate"]):
        if "RUNTIME" not in dependency["scopes"]:
            continue
        ids = []
        for declared in dependency["licenses"]:
            spdx = declared.get("spdx") or declared["name"]
            ids.append(spdx)
            if spdx not in licenses:
                text = ASSETS / "texts" / f"{spdx}.txt"
                licenses[spdx] = {
                    "id": spdx,
                    "name": declared["name"],
                    "url": declared.get("url"),
                    "text": f"licenses/texts/{spdx}.txt" if text.is_file() else None,
                }
        components.append({"coordinate": dependency["coordinate"], "licenses": sorted(set(ids))})
    return {
        "generatedFrom": "build/reports/licenses/license-report.json (./gradlew licenseCheck), RUNTIME scope",
        "licenses": [licenses[key] for key in sorted(licenses)],
        "components": components,
    }


def render(catalog):
    return json.dumps(catalog, ensure_ascii=False, indent=2) + "\n"


def main(argv):
    if not REPORT.is_file():
        print(f"missing {REPORT}; run ./gradlew licenseCheck first", file=sys.stderr)
        return 2
    expected = render(build_catalog(json.loads(REPORT.read_text(encoding="utf-8"))))
    if "--check" in argv:
        actual = CATALOG.read_text(encoding="utf-8") if CATALOG.is_file() else ""
        if actual != expected:
            print(f"{CATALOG} is stale; run python3 tools/licenses/about_licenses.py", file=sys.stderr)
            return 1
        print("license catalog is up to date")
        return 0
    CATALOG.parent.mkdir(parents=True, exist_ok=True)
    CATALOG.write_text(expected, encoding="utf-8")
    print(f"wrote {CATALOG}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
