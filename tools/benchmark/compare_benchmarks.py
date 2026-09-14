#!/usr/bin/env python3
"""Nightly benchmark regression gate (T-1801, plan §9: a regression of more than 10 % fails).

Compares AndroidX Benchmark JSON results (``*benchmarkData.json``) with the committed baselines of the same format.
For every benchmark present in both, each metric's median (``metrics``) or P50 (``sampledMetrics``) may grow by at
most the threshold. Benchmarks without a baseline are listed and pass, so a new baseline can be committed from the
uploaded results after review. Exit code 1 means at least one regression.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


def load(directory: Path) -> dict[tuple[str, str], dict[str, float]]:
    """Every benchmark in the JSON files below ``directory`` as (class, name) → metric → typical value."""
    results: dict[tuple[str, str], dict[str, float]] = {}
    for path in sorted(directory.rglob("*benchmarkData.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        for benchmark in data.get("benchmarks", []):
            key = (benchmark.get("className", ""), benchmark.get("name", ""))
            values = {
                name: float(metric["median"])
                for name, metric in benchmark.get("metrics", {}).items()
                if "median" in metric
            }
            values.update(
                {
                    name: float(metric["P50"])
                    for name, metric in benchmark.get("sampledMetrics", {}).items()
                    if "P50" in metric
                }
            )
            results[key] = values
    return results


def regressions(
    baseline: dict[tuple[str, str], dict[str, float]],
    current: dict[tuple[str, str], dict[str, float]],
    threshold: float,
) -> list[str]:
    """Human-readable lines for every metric that grew by more than ``threshold`` over a positive baseline."""
    found = []
    for key, metrics in sorted(current.items()):
        for name, value in sorted(metrics.items()):
            reference = baseline.get(key, {}).get(name)
            if reference is not None and reference > 0 and value > reference * (1 + threshold):
                growth = (value / reference - 1) * 100
                found.append(f"{key[0]}.{key[1]} {name}: {reference:g} → {value:g} (+{growth:.1f} %)")
    return found


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--baseline", type=Path, required=True)
    parser.add_argument("--results", type=Path, required=True)
    parser.add_argument("--threshold", type=float, default=0.10)
    args = parser.parse_args(argv)
    current = load(args.results)
    if not current:
        print(f"No benchmark results under {args.results}", file=sys.stderr)
        return 1
    baseline = load(args.baseline) if args.baseline.is_dir() else {}
    for key in sorted(set(current) - set(baseline)):
        print(f"No baseline for {key[0]}.{key[1]}; recorded only")
    found = regressions(baseline, current, args.threshold)
    for line in found:
        print(f"Regression: {line}")
    return 1 if found else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
