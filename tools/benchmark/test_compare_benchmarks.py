"""Tests of the nightly benchmark regression gate: python3 -m unittest discover tools/benchmark."""
import json
import tempfile
import unittest
from pathlib import Path

import compare_benchmarks


def write(directory: Path, cold_median: float, frame_p50: float) -> None:
    data = {
        "benchmarks": [
            {
                "className": "ir.taqvim.benchmark.StartupBenchmark",
                "name": "startupCold",
                "metrics": {"timeToInitialFrameMs": {"minimum": 1.0, "median": cold_median}},
                "sampledMetrics": {"frameDurationCpuMs": {"P50": frame_p50, "P90": frame_p50 * 2}},
            }
        ]
    }
    directory.mkdir(parents=True, exist_ok=True)
    (directory / "ir.taqvim.benchmark-benchmarkData.json").write_text(json.dumps(data), encoding="utf-8")


class CompareBenchmarksTest(unittest.TestCase):
    def run_gate(self, baseline: tuple[float, float] | None, current: tuple[float, float]) -> int:
        with tempfile.TemporaryDirectory() as root:
            base, results = Path(root, "baselines"), Path(root, "results")
            if baseline is not None:
                write(base, *baseline)
            write(results, *current)
            return compare_benchmarks.main(["--baseline", str(base), "--results", str(results)])

    def test_growth_within_ten_percent_passes(self) -> None:
        self.assertEqual(self.run_gate((300.0, 8.0), (329.0, 8.7)), 0)

    def test_growth_over_ten_percent_fails(self) -> None:
        self.assertEqual(self.run_gate((300.0, 8.0), (331.0, 8.0)), 1)
        self.assertEqual(self.run_gate((300.0, 8.0), (300.0, 8.9)), 1)

    def test_missing_baseline_only_records(self) -> None:
        self.assertEqual(self.run_gate(None, (900.0, 30.0)), 0)

    def test_missing_results_fail(self) -> None:
        with tempfile.TemporaryDirectory() as root:
            self.assertEqual(compare_benchmarks.main(["--baseline", root, "--results", root]), 1)

    def test_memory_budget_sums_rss_metrics(self) -> None:
        budgets = {
            "_comment": "ignored",
            "a.Memory.month": {"metricPrefixes": ["memoryRssAnon", "memoryRssFile"], "maximum": 81920},
        }
        within = {("a.Memory", "month"): {"memoryRssAnonMaxKb": 50000.0, "memoryRssFileMaxKb": 30000.0}}
        over = {("a.Memory", "month"): {"memoryRssAnonMaxKb": 60000.0, "memoryRssFileMaxKb": 30000.0}}
        renamed = {("a.Memory", "month"): {"memoryHeapSizeMaxKb": 1.0}}
        self.assertEqual(compare_benchmarks.over_budget(budgets, within), [])
        self.assertEqual(len(compare_benchmarks.over_budget(budgets, over)), 1)
        self.assertEqual(len(compare_benchmarks.over_budget(budgets, renamed)), 1)
        self.assertEqual(compare_benchmarks.over_budget(budgets, {}), [])

    def test_budget_file_fails_the_gate(self) -> None:
        with tempfile.TemporaryDirectory() as root:
            results, budget_file = Path(root, "results"), Path(root, "budgets.json")
            write(results, 300.0, 8.0)
            startup = "ir.taqvim.benchmark.StartupBenchmark.startupCold"
            budget_file.write_text(
                json.dumps({startup: {"metricPrefixes": ["timeToInitialFrame"], "maximum": 250}}), encoding="utf-8"
            )
            arguments = ["--baseline", str(Path(root, "none")), "--results", str(results)]
            self.assertEqual(compare_benchmarks.main([*arguments, "--budgets", str(budget_file)]), 1)


if __name__ == "__main__":
    unittest.main()
