#!/usr/bin/env python3
"""Nightly benchmark regression gate (T-1801, plan §9: a regression of more than 10 % fails).

Compares AndroidX Benchmark JSON results (``*benchmarkData.json``) with the committed baselines of the same format.
For every benchmark present in both, each metric's median (``metrics``) or P50 (``sampledMetrics``) may grow by at
most the threshold. Benchmarks without a baseline are listed and pass, so a new baseline can be committed from the
uploaded results after review.

Lost results fail too (review finding B14):

- with ``--required``, every required benchmark must be reported with each of its metric prefixes, and a result that
  is neither required nor optional fails, so a renamed test or metric cannot pass silently;
- a benchmark or metric present in the baseline but missing from the results fails, unless the benchmark is optional;
- a benchmark reported more than once fails, because only one of the copies could be compared.

Exit code 1 means at least one failure.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

Results = dict[tuple[str, str], dict[str, float]]


def typical_values(benchmark: dict) -> dict[str, float]:
    """The median of each ``metrics`` entry and the P50 of each ``sampledMetrics`` entry."""
    values = {
        name: float(metric["median"]) for name, metric in benchmark.get("metrics", {}).items() if "median" in metric
    }
    values.update(
        {name: float(metric["P50"]) for name, metric in benchmark.get("sampledMetrics", {}).items() if "P50" in metric}
    )
    return values


def load_with_duplicates(directory: Path) -> tuple[Results, list[str]]:
    """Every benchmark below ``directory`` as (class, name) → metric → typical value, and the names seen twice."""
    results: Results = {}
    duplicates = []
    for path in sorted(directory.rglob("*benchmarkData.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        for benchmark in data.get("benchmarks", []):
            key = (benchmark.get("className", ""), benchmark.get("name", ""))
            if key in results:
                duplicates.append(f"{key[0]}.{key[1]}")
            results[key] = typical_values(benchmark)
    return results, duplicates


def load(directory: Path) -> Results:
    """Every benchmark in the JSON files below ``directory`` as (class, name) → metric → typical value."""
    return load_with_duplicates(directory)[0]


def regressions(baseline: Results, current: Results, threshold: float) -> list[str]:
    """Human-readable lines for every metric that grew by more than ``threshold`` over a positive baseline."""
    found = []
    for key, metrics in sorted(current.items()):
        for name, value in sorted(metrics.items()):
            reference = baseline.get(key, {}).get(name)
            if reference is not None and reference > 0 and value > reference * (1 + threshold):
                growth = (value / reference - 1) * 100
                found.append(f"{key[0]}.{key[1]} {name}: {reference:g} → {value:g} (+{growth:.1f} %)")
    return found


def lost_since_baseline(baseline: Results, current: Results, optional: set[str]) -> list[str]:
    """Lines for baseline benchmarks and metrics that the results no longer report (optional benchmarks excepted)."""
    found = []
    for key, metrics in sorted(baseline.items()):
        name = f"{key[0]}.{key[1]}"
        if key not in current:
            if name not in optional:
                found.append(f"{name}: in the baseline but not reported")
            continue
        lost = sorted(set(metrics) - set(current[key]))
        found.extend(f"{name} {metric}: in the baseline but not reported" for metric in lost)
    return found


def missing_required(required: dict, current: Results) -> list[str]:
    """Lines for required benchmarks or metric prefixes that were not reported, and for unlisted results."""
    expected: dict[str, list[str]] = required.get("required", {})
    listed = set(expected) | set(required.get("optional", {}))
    found = []
    for name, prefixes in sorted(expected.items()):
        class_name, _, test = name.rpartition(".")
        metrics = current.get((class_name, test))
        if metrics is None:
            found.append(f"{name}: required but not reported")
            continue
        found.extend(
            f"{name}: no metric starting with {prefix}"
            for prefix in prefixes
            if not any(metric.startswith(prefix) for metric in metrics)
        )
    found.extend(
        f"{key[0]}.{key[1]}: reported but not listed as required or optional"
        for key in sorted(current)
        if f"{key[0]}.{key[1]}" not in listed
    )
    return found


def over_budget(budgets: dict[str, dict], current: Results) -> list[str]:
    """Lines for every budgeted benchmark (``className.testName``) whose summed budget metrics reach the maximum.

    A budget sums the metrics whose names start with one of ``metricPrefixes`` (T-1803: anonymous plus file RSS). A
    benchmark that ran but reported none of them fails too. A budgeted benchmark that did not run is left to the
    required-benchmark check.
    """
    found = []
    for name, budget in sorted(budgets.items()):
        if name.startswith("_"):
            continue
        class_name, _, test = name.rpartition(".")
        metrics = current.get((class_name, test))
        if metrics is None:
            continue
        prefixes = budget["metricPrefixes"]
        parts = [value for metric, value in metrics.items() if any(metric.startswith(p) for p in prefixes)]
        if not parts:
            found.append(f"{name}: none of {prefixes} were measured")
        elif sum(parts) >= budget["maximum"]:
            found.append(f"{name}: {sum(parts):g} ≥ budget {budget['maximum']:g} {budget.get('unit', '')}".rstrip())
    return found


def read_json(path: Path | None) -> dict:
    return json.loads(path.read_text(encoding="utf-8")) if path else {}


def failures(args: argparse.Namespace, current: Results, duplicates: list[str]) -> list[str]:
    """Every failure line of the gate, each with its category prefix."""
    required = read_json(args.required)
    baseline = load(args.baseline) if args.baseline.is_dir() else {}
    for key in sorted(set(current) - set(baseline)):
        print(f"No baseline for {key[0]}.{key[1]}; recorded only")
    optional = set(required.get("optional", {}))
    return (
        [f"Duplicate result: {name}" for name in duplicates]
        + [f"Missing: {line}" for line in (missing_required(required, current) if args.required else [])]
        + [f"Missing: {line}" for line in lost_since_baseline(baseline, current, optional)]
        + [f"Regression: {line}" for line in regressions(baseline, current, args.threshold)]
        + [f"Over budget: {line}" for line in over_budget(read_json(args.budgets), current)]
    )


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--baseline", type=Path, required=True)
    parser.add_argument("--results", type=Path, required=True)
    parser.add_argument("--threshold", type=float, default=0.10)
    parser.add_argument("--budgets", type=Path, help="JSON of absolute budgets (plan §9), optional")
    parser.add_argument("--required", type=Path, help="JSON of required and optional benchmarks, optional")
    args = parser.parse_args(argv)
    current, duplicates = load_with_duplicates(args.results)
    if not current:
        print(f"No benchmark results under {args.results}", file=sys.stderr)
        return 1
    found = failures(args, current, duplicates)
    for line in found:
        print(line)
    return 1 if found else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
