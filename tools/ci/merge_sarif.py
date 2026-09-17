#!/usr/bin/env python3
"""Merges the per-module SARIF reports of each tool into one run per tool.

GitHub code scanning accepts a single SARIF run per upload category, while detekt and Android Lint write one report
per module. Usage: merge_sarif.py <output-dir> <sarif>...; writes <output-dir>/<tool>.sarif for every tool found.
"""
import json
import pathlib
import re
import sys


def _strip_indices(value):
    """Drops rule and artifact indices, which only point into the run they came from."""
    if isinstance(value, dict):
        return {k: _strip_indices(v) for k, v in value.items() if k not in ("ruleIndex", "index")}
    if isinstance(value, list):
        return [_strip_indices(v) for v in value]
    return value


def merge(documents):
    """Returns {tool name: merged SARIF document} for the given parsed SARIF documents."""
    merged = {}
    for document in documents:
        for run in document.get("runs", []):
            driver = run["tool"]["driver"]
            name = driver["name"]
            target = merged.get(name)
            if target is None:
                target = {
                    "$schema": document.get("$schema", "https://json.schemastore.org/sarif-2.1.0.json"),
                    "version": document.get("version", "2.1.0"),
                    "runs": [{
                        "tool": {"driver": {**{k: v for k, v in driver.items() if k != "rules"}, "rules": []}},
                        "results": [],
                    }],
                }
                merged[name] = target
            out = target["runs"][0]
            known = {rule["id"] for rule in out["tool"]["driver"]["rules"]}
            for rule in driver.get("rules", []):
                if rule["id"] not in known:
                    out["tool"]["driver"]["rules"].append(rule)
                    known.add(rule["id"])
            if run.get("originalUriBaseIds"):
                out.setdefault("originalUriBaseIds", {}).update(run["originalUriBaseIds"])
            out["results"].extend(_strip_indices(run.get("results", [])))
    return merged


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    output = pathlib.Path(argv[0])
    output.mkdir(parents=True, exist_ok=True)
    documents = [json.loads(pathlib.Path(p).read_text(encoding="utf-8")) for p in argv[1:]]
    for name, document in merge(documents).items():
        slug = re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")
        (output / f"{slug}.sarif").write_text(json.dumps(document), encoding="utf-8")
        print(f"{slug}: {len(document['runs'][0]['results'])} results")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
