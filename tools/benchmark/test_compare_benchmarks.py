"""Tests of the nightly benchmark regression gate: python3 -m unittest discover tools/benchmark."""
import json
import re
import tempfile
import unittest
from pathlib import Path

import compare_benchmarks

BUDGETS = Path(__file__).resolve().parents[2] / "benchmark" / "budgets.json"
REQUIRED = BUDGETS.parent / "required.json"
STARTUP = ("ir.taqvim.benchmark.StartupBenchmark", "startupCold")
MASK = ("ir.taqvim.benchmark.micro.MapMaskBenchmark", "dayNightMask")


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

    def test_time_budget_of_widget_render(self) -> None:
        render = "ir.taqvim.benchmark.micro.WidgetRenderBenchmark"
        budgets = json.loads(BUDGETS.read_text(encoding="utf-8"))

        def timed(median: float) -> dict[tuple[str, str], dict[str, float]]:
            return {(render, "monthBitmap4x4"): {"timeNs": median}}

        self.assertEqual(compare_benchmarks.over_budget(budgets, timed(29_999_999.0)), [])
        self.assertEqual(len(compare_benchmarks.over_budget(budgets, timed(30_000_000.0))), 1)

    def test_micro_results_are_loaded_by_median(self) -> None:
        with tempfile.TemporaryDirectory() as root:
            data = {
                "benchmarks": [
                    {
                        "className": "ir.taqvim.benchmark.micro.MapMaskBenchmark",
                        "name": "dayNightMask",
                        "metrics": {"timeNs": {"minimum": 1.0, "maximum": 9.0, "median": 4.0, "runs": [1, 4, 9]}},
                    }
                ]
            }
            Path(root, "micro.dayNightMask-benchmarkData.json").write_text(json.dumps(data), encoding="utf-8")
            loaded = compare_benchmarks.load(Path(root))
            self.assertEqual(loaded, {("ir.taqvim.benchmark.micro.MapMaskBenchmark", "dayNightMask"): {"timeNs": 4.0}})

    def write_results(self, directory: Path, *benchmarks: dict) -> None:
        directory.mkdir(parents=True, exist_ok=True)
        for index, benchmark in enumerate(benchmarks):
            data = {"benchmarks": [benchmark]}
            Path(directory, f"r{index}-benchmarkData.json").write_text(json.dumps(data), encoding="utf-8")

    @staticmethod
    def result(key: tuple[str, str], **medians: float) -> dict:
        metrics = {name: {"median": value} for name, value in medians.items()}
        return {"className": key[0], "name": key[1], "metrics": metrics}

    def gate(self, baseline: list[dict], current: list[dict], required: dict | None) -> int:
        with tempfile.TemporaryDirectory() as root:
            base, results = Path(root, "baselines"), Path(root, "results")
            self.write_results(base, *baseline)
            self.write_results(results, *current)
            arguments = ["--baseline", str(base), "--results", str(results)]
            if required is not None:
                required_file = Path(root, "required.json")
                required_file.write_text(json.dumps(required), encoding="utf-8")
                arguments += ["--required", str(required_file)]
            return compare_benchmarks.main(arguments)

    def test_missing_required_startup_fails_even_with_other_results(self) -> None:
        required = {"required": {".".join(STARTUP): ["timeToInitialFrame"], ".".join(MASK): ["timeNs"]}}
        mask = self.result(MASK, timeNs=4.0)
        startup = self.result(STARTUP, timeToInitialFrameMs=300.0)
        self.assertEqual(self.gate([], [mask], required), 1)
        self.assertEqual(self.gate([], [mask, startup], required), 0)

    def test_missing_required_metric_fails(self) -> None:
        required = {"required": {".".join(STARTUP): ["timeToInitialFrame"]}}
        self.assertEqual(self.gate([], [self.result(STARTUP, somethingElseMs=300.0)], required), 1)

    def test_metric_removed_from_the_baseline_fails(self) -> None:
        baseline = [self.result(STARTUP, timeToInitialFrameMs=300.0, timeToFullDisplayMs=500.0)]
        current = [self.result(STARTUP, timeToInitialFrameMs=300.0)]
        self.assertEqual(self.gate(baseline, current, None), 1)
        self.assertEqual(self.gate(baseline, baseline, None), 0)

    def test_renamed_test_fails(self) -> None:
        renamed = (STARTUP[0], "startupColdRenamed")
        required = {"required": {".".join(STARTUP): ["timeToInitialFrame"]}}
        current = [self.result(renamed, timeToInitialFrameMs=300.0)]
        self.assertEqual(self.gate([self.result(STARTUP, timeToInitialFrameMs=300.0)], current, None), 1)
        self.assertEqual(self.gate([], current, required), 1)

    def test_duplicated_results_fail(self) -> None:
        startup = self.result(STARTUP, timeToInitialFrameMs=300.0)
        self.assertEqual(self.gate([], [startup, startup], None), 1)
        self.assertEqual(self.gate([], [startup], None), 0)

    def test_optional_benchmarks_may_be_missing_or_reported(self) -> None:
        generator = ("ir.taqvim.benchmark.BaselineProfileGenerator", "startup")
        required = {
            "required": {".".join(STARTUP): ["timeToInitialFrame"]},
            "optional": {".".join(generator): "run on demand"},
        }
        startup = self.result(STARTUP, timeToInitialFrameMs=300.0)
        profile = self.result(generator, timeNs=1.0)
        self.assertEqual(self.gate([profile], [startup], required), 0)
        self.assertEqual(self.gate([], [startup, profile], required), 0)

    def test_required_file_lists_every_benchmark_test(self) -> None:
        required = json.loads(REQUIRED.read_text(encoding="utf-8"))
        listed = set(required["required"]) | set(required["optional"])
        declared = set()
        for path in BUDGETS.parent.rglob("*.kt"):
            text = path.read_text(encoding="utf-8")
            package = re.search(r"^package (\S+)$", text, re.MULTILINE)
            classes = re.findall(r"^class (\w+)", text, re.MULTILINE)
            for test in re.findall(r"@Test\s+fun (\w+)\(", text):
                self.assertIsNotNone(package)
                declared.add(f"{package.group(1)}.{classes[0]}.{test}")
        self.assertEqual(declared, listed)
        budgets = {name for name in json.loads(BUDGETS.read_text(encoding="utf-8")) if not name.startswith("_")}
        self.assertLessEqual(budgets, set(required["required"]))

    def test_budget_file_names_existing_benchmarks(self) -> None:
        budgets = json.loads(BUDGETS.read_text(encoding="utf-8"))
        sources = "\n".join(path.read_text(encoding="utf-8") for path in BUDGETS.parent.rglob("*.kt"))
        for name, budget in budgets.items():
            if name.startswith("_"):
                continue
            class_name, _, test = name.rpartition(".")
            with self.subTest(budget=name):
                self.assertIn(f"package {class_name.rpartition('.')[0]}\n", sources)
                self.assertIn(f"class {class_name.rpartition('.')[2]} ", sources)
                self.assertIn(f"fun {test}()", sources)
                self.assertTrue(budget["metricPrefixes"])
                self.assertGreater(budget["maximum"], 0)


if __name__ == "__main__":
    unittest.main()
