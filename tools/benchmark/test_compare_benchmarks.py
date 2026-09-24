"""Tests of the nightly benchmark regression gate: python3 -m unittest discover tools/benchmark.

Every call into the gate goes through :func:`gate`, which captures its stdout. The fixtures use realistic benchmark
names and plausible millisecond values, so printing them would put lines like ``Regression: …StartupBenchmark…`` into
the same CI log as the real comparison, where a reader cannot tell them apart: that cost a day of chasing a 31 ms
cold-start regression that only ever existed in ``test_growth_over_ten_percent_fails``. Assert on the captured text
instead of letting it escape.
"""
import contextlib
import io
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


def gate(arguments: list[str]) -> tuple[int, str]:
    """Runs the gate with its stdout captured, so test fixtures never reach the CI log. Returns (exit code, output)."""
    captured = io.StringIO()
    with contextlib.redirect_stdout(captured), contextlib.redirect_stderr(captured):
        code = compare_benchmarks.main(arguments)
    return code, captured.getvalue()


class CompareBenchmarksTest(unittest.TestCase):
    def run_gate(self, baseline: tuple[float, float] | None, current: tuple[float, float]) -> int:
        with tempfile.TemporaryDirectory() as root:
            base, results = Path(root, "baselines"), Path(root, "results")
            if baseline is not None:
                write(base, *baseline)
            write(results, *current)
            return gate(["--baseline", str(base), "--results", str(results)])[0]

    def test_growth_within_ten_percent_passes(self) -> None:
        self.assertEqual(self.run_gate((300.0, 8.0), (329.0, 8.7)), 0)

    def test_growth_over_ten_percent_fails(self) -> None:
        self.assertEqual(self.run_gate((300.0, 8.0), (331.0, 8.0)), 1)
        self.assertEqual(self.run_gate((300.0, 8.0), (300.0, 8.9)), 1)

    def test_missing_baseline_only_records(self) -> None:
        self.assertEqual(self.run_gate(None, (900.0, 30.0)), 0)

    def test_missing_results_fail(self) -> None:
        with tempfile.TemporaryDirectory() as root:
            self.assertEqual(gate(["--baseline", root, "--results", root])[0], 1)

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

    def test_physical_device_budgets_are_skipped_on_an_emulator(self) -> None:
        budgets = {
            "ir.taqvim.Memory.rss": {
                "metricPrefixes": ["memoryRss"],
                "maximum": 10.0,
                "physicalDeviceOnly": True,
            },
        }
        over = {("ir.taqvim.Memory", "rss"): {"memoryRssAnonMaxKb": 99.0}}
        self.assertEqual(compare_benchmarks.over_budget(budgets, over, physical_device=False), [])
        self.assertEqual(len(compare_benchmarks.over_budget(budgets, over, physical_device=True)), 1)
        self.assertEqual(compare_benchmarks.device_only(budgets), ["ir.taqvim.Memory.rss"])

    def test_budget_file_fails_the_gate(self) -> None:
        with tempfile.TemporaryDirectory() as root:
            results, budget_file = Path(root, "results"), Path(root, "budgets.json")
            write(results, 300.0, 8.0)
            startup = "ir.taqvim.benchmark.StartupBenchmark.startupCold"
            budget_file.write_text(
                json.dumps({startup: {"metricPrefixes": ["timeToInitialFrame"], "maximum": 250}}), encoding="utf-8"
            )
            arguments = ["--baseline", str(Path(root, "none")), "--results", str(results)]
            self.assertEqual(gate([*arguments, "--budgets", str(budget_file)])[0], 1)

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
            return gate(arguments)[0]

    def test_missing_required_startup_fails_even_with_other_results(self) -> None:
        required = {"required": {".".join(STARTUP): ["timeToInitialFrame"], ".".join(MASK): ["timeNs"]}}
        mask = self.result(MASK, timeNs=4.0)
        startup = self.result(STARTUP, timeToInitialFrameMs=300.0)
        self.assertEqual(self.gate([mask, startup], [mask], required), 1)
        self.assertEqual(self.gate([mask, startup], [mask, startup], required), 0)

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
        self.assertEqual(self.gate([profile, startup], [startup], required), 0)
        self.assertEqual(self.gate([startup], [startup, profile], required), 0)

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

    # REVIEW R08: a gate with no baselines and no absolute startup budget passed a synthetic 60-second start.

    def full_run(self, startup_ms: float, other: float = 1.0) -> list[dict]:
        """A result for every required benchmark and metric: startups at [startup_ms], everything else [other]."""
        required = json.loads(REQUIRED.read_text(encoding="utf-8"))["required"]
        results = []
        for name, prefixes in required.items():
            class_name, _, test = name.rpartition(".")
            value = startup_ms if "StartupBenchmark" in class_name else other
            metrics = {f"{prefix}{'Ms' if not prefix.endswith('Ms') else ''}": value for prefix in prefixes}
            benchmark = self.result((class_name, test), **metrics)
            if "frameCount" in prefixes:
                # Frame journeys report their frame times as sampled metrics; 10 ms keeps them within every budget.
                benchmark["sampledMetrics"] = {"frameDurationCpuMs": {"P50": 5.0, "P99": 10.0}}
            results.append(benchmark)
        return results

    def repository_gate(self, baseline: list[dict], current: list[dict], *extra: str) -> int:
        with tempfile.TemporaryDirectory() as root:
            base, results = Path(root, "baselines"), Path(root, "results")
            self.write_results(base, *baseline)
            self.write_results(results, *current)
            arguments = ["--baseline", str(base), "--results", str(results), "--threshold", "0.10",
                         "--budgets", str(BUDGETS), "--required", str(REQUIRED), *extra]
            return gate(arguments)[0]

    def test_reviewers_synthetic_sixty_second_start_fails(self) -> None:
        # The exact case of the review: every required benchmark and metric, startups 60 000 ms, the rest 1, no
        # baselines, the nightly command line.
        self.assertEqual(self.repository_gate([], self.full_run(60000.0)), 1)

    def test_sixty_second_start_fails_even_against_a_baseline_of_itself(self) -> None:
        run = self.full_run(60000.0)
        self.assertEqual(self.repository_gate(run, run), 1)

    def test_sixty_second_start_fails_in_recording_mode(self) -> None:
        self.assertEqual(self.repository_gate([], self.full_run(60000.0), "--record-baseline"), 1)

    def test_missing_baseline_of_a_required_benchmark_fails(self) -> None:
        run = self.full_run(800.0)
        self.assertEqual(self.repository_gate([], run), 1)
        self.assertEqual(self.repository_gate(run, run), 0)

    def test_recording_mode_is_never_qualifying(self) -> None:
        run = self.full_run(800.0)
        self.assertEqual(self.repository_gate([], run, "--record-baseline"), compare_benchmarks.RECORDED_EXIT)
        self.assertNotEqual(compare_benchmarks.RECORDED_EXIT, 0)

    def test_hosted_ceiling_and_device_budget_of_cold_start(self) -> None:
        # 800 ms is what the hosted emulator measures: within its ceiling, far over the §9 device budget.
        run = self.full_run(800.0)
        self.assertEqual(self.repository_gate(run, run), 0)
        self.assertEqual(self.repository_gate(run, run, "--physical-device"), 1)
        fast = self.full_run(300.0)
        self.assertEqual(self.repository_gate(fast, fast, "--physical-device"), 0)

    def test_p99_feeds_the_jank_budget_but_not_the_regression_gate(self) -> None:
        scroll = ("ir.taqvim.benchmark.MonthPagerScrollBenchmark", "scrollTwentyFourMonths")

        def with_p99(p99: float) -> list[dict]:
            run = [b for b in self.full_run(300.0) if (b["className"], b["name"]) != scroll]
            frames = self.result(scroll, frameCountMs=100.0)
            frames["metrics"] = {"frameCount": {"median": 100.0}}
            frames["sampledMetrics"] = {"frameDurationCpuMs": {"P50": 5.0, "P99": p99}}
            return run + [frames]

        # P99 doubled against the baseline: not a regression, and still under both limits on a hosted run.
        self.assertEqual(self.repository_gate(with_p99(100.0), with_p99(200.0)), 0)
        # Over the hosted ceiling fails; on a device, over 16 ms fails and under it passes.
        self.assertEqual(self.repository_gate(with_p99(100.0), with_p99(400.0)), 1)
        self.assertEqual(self.repository_gate(with_p99(10.0), with_p99(20.0), "--physical-device"), 1)
        self.assertEqual(self.repository_gate(with_p99(10.0), with_p99(12.0), "--physical-device"), 0)

    def repository_output(self, baseline: list[dict], current: list[dict], *extra: str) -> tuple[int, str]:
        """[repository_gate] but keeping the gate's captured output, for the control's wording."""
        with tempfile.TemporaryDirectory() as root:
            base, results = Path(root, "baselines"), Path(root, "results")
            self.write_results(base, *baseline)
            self.write_results(results, *current)
            return gate(
                ["--baseline", str(base), "--results", str(results), "--threshold", "0.10",
                 "--budgets", str(BUDGETS), "--required", str(REQUIRED), *extra]
            )

    def degraded(self, *moves: tuple[str, float]) -> list[dict]:
        """A full run where the named control benchmarks are multiplied, as a degraded runner multiplies them."""
        factors = dict(moves)
        results = []
        for benchmark in self.full_run(300.0):
            name = f"{benchmark['className']}.{benchmark['name']}"
            factor = factors.get(name)
            if factor is not None:
                benchmark["metrics"] = {k: {"median": v["median"] * factor} for k, v in benchmark["metrics"].items()}
            results.append(benchmark)
        return results

    def test_a_degraded_runner_is_inconclusive_rather_than_a_regression(self) -> None:
        # Run 36040359691: progressRingBitmap +258 % and moonBitmap +76 % over code byte-identical to the baseline,
        # alongside 10 "regressions" in the app's journeys. The machine moved, so none of it is evidence.
        baseline = self.full_run(300.0)
        degraded = self.degraded(
            ("ir.taqvim.benchmark.micro.WidgetRenderBenchmark.progressRingBitmap", 3.584),
            ("ir.taqvim.benchmark.micro.WidgetRenderBenchmark.moonBitmap", 1.756),
        )
        # The app's own journeys also look slower on that machine.
        for benchmark in degraded:
            if benchmark["className"].endswith("ScreenScrollBenchmarks"):
                benchmark["metrics"] = {k: {"median": v["median"] * 1.4} for k, v in benchmark["metrics"].items()}
        code, output = self.repository_output(baseline, degraded)
        self.assertEqual(code, compare_benchmarks.INCONCLUSIVE_EXIT)
        self.assertIn("INCONCLUSIVE", output)
        self.assertIn("progressRingBitmap", output)
        self.assertNotIn("Regression:", output)

    def test_run_35953669737_is_inconclusive(self) -> None:
        # The earlier degraded run: sunArcBitmap +349 %, progressRingBitmap +241 %.
        code, output = self.repository_output(
            self.full_run(300.0),
            self.degraded(
                ("ir.taqvim.benchmark.micro.WidgetRenderBenchmark.sunArcBitmap", 4.488),
                ("ir.taqvim.benchmark.micro.WidgetRenderBenchmark.progressRingBitmap", 3.413),
            ),
        )
        self.assertEqual(code, compare_benchmarks.INCONCLUSIVE_EXIT)
        self.assertIn("Control moved", output)

    def test_a_healthy_runner_still_fails_a_real_regression(self) -> None:
        # Controls steady, one journey 30 % slower: exactly what the gate exists to catch.
        baseline = self.full_run(300.0)
        current = [json.loads(json.dumps(b)) for b in baseline]
        for benchmark in current:
            if benchmark["name"] == "timelineScroll":
                benchmark["metrics"] = {k: {"median": v["median"] * 1.30} for k, v in benchmark["metrics"].items()}
        code, output = self.repository_output(baseline, current)
        self.assertEqual(code, 1)
        self.assertIn("Regression:", output)
        self.assertNotIn("INCONCLUSIVE", output)

    def test_a_control_within_its_band_does_not_excuse_a_regression(self) -> None:
        # A control 20 % off is ordinary emulator noise, well inside the 50 % band: the gate must still fail.
        baseline = self.full_run(300.0)
        current = self.degraded(("ir.taqvim.benchmark.micro.WidgetRenderBenchmark.moonBitmap", 1.20))
        for benchmark in current:
            if benchmark["name"] == "timelineScroll":
                benchmark["metrics"] = {k: {"median": v["median"] * 1.30} for k, v in benchmark["metrics"].items()}
        code, output = self.repository_output(baseline, current)
        self.assertEqual(code, 1)
        self.assertIn("Regression:", output)

    def test_a_structural_failure_holds_on_a_degraded_runner(self) -> None:
        # A duplicate result does not depend on timing, so it must fail even when the control says inconclusive.
        degraded = self.degraded(("ir.taqvim.benchmark.micro.WidgetRenderBenchmark.sunArcBitmap", 4.0))
        duplicate = next(b for b in degraded if b["name"] == "sunArcBitmap")
        code, output = self.repository_output(self.full_run(300.0), degraded + [json.loads(json.dumps(duplicate))])
        self.assertEqual(code, 1)
        self.assertIn("Duplicate result", output)

    def test_every_control_benchmark_is_required_and_synthetic(self) -> None:
        required = json.loads(REQUIRED.read_text(encoding="utf-8"))
        control = required["control"]["benchmarks"]
        self.assertTrue(control)
        for name in control:
            with self.subTest(name=name):
                self.assertIn(name, required["required"])
                # Only the in-process microbenchmarks qualify: they draw fixed synthetic work with no UI or dataset.
                self.assertIn(".micro.", name)

    def test_hosted_ceilings_sit_above_the_device_budgets(self) -> None:
        budgets = json.loads(BUDGETS.read_text(encoding="utf-8"))
        for name, budget in budgets.items():
            if name.startswith("_") or "hostedMaximum" not in budget:
                continue
            with self.subTest(name=name):
                self.assertTrue(budget.get("physicalDeviceOnly"))
                self.assertGreater(budget["hostedMaximum"], budget["maximum"])


if __name__ == "__main__":
    unittest.main()
