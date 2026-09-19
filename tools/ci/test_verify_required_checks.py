"""Tests for verify_required_checks.py: the release gate fails closed."""
import pathlib
import tempfile
import unittest

import verify_required_checks as checks


def run(name, run_id, status="completed", conclusion="success"):
    return {"name": name, "id": run_id, "status": status, "conclusion": conclusion}


class VerdictsTest(unittest.TestCase):
    def ok(self, required, runs):
        return all(ok for _, ok, _ in checks.verdicts(required, runs))

    def test_all_required_success_passes(self):
        self.assertTrue(self.ok(["A", "B"], [run("A", 1), run("B", 2), run("Unrelated", 3, conclusion="failure")]))

    def test_missing_check_fails(self):
        self.assertFalse(self.ok(["A", "B"], [run("A", 1)]))

    def test_running_check_fails(self):
        self.assertFalse(self.ok(["A"], [run("A", 1, status="in_progress", conclusion=None)]))

    def test_queued_check_fails(self):
        self.assertFalse(self.ok(["A"], [run("A", 1, status="queued", conclusion=None)]))

    def test_every_non_success_conclusion_fails(self):
        for conclusion in ["failure", "cancelled", "skipped", "neutral", "timed_out", "action_required", "stale"]:
            with self.subTest(conclusion=conclusion):
                self.assertFalse(self.ok(["A"], [run("A", 1, conclusion=conclusion)]))

    def test_latest_rerun_decides_both_ways(self):
        self.assertTrue(self.ok(["A"], [run("A", 1, conclusion="failure"), run("A", 2)]))
        self.assertFalse(self.ok(["A"], [run("A", 2, conclusion="failure"), run("A", 1)]))

    def test_reasons_name_the_problem(self):
        result = dict((name, reason) for name, _, reason in checks.verdicts(["A", "B"], [run("A", 1, conclusion="skipped")]))
        self.assertIn("skipped", result["A"])
        self.assertIn("missing", result["B"])


class RequiredFileTest(unittest.TestCase):
    def test_comments_and_blank_lines_are_ignored(self):
        with tempfile.TemporaryDirectory() as directory:
            path = pathlib.Path(directory, "required.txt")
            path.write_text("# comment\n\nAPI 26\n  Wear OS 34  \n", encoding="utf-8")
            self.assertEqual(checks.read_required(path), ["API 26", "Wear OS 34"])

    def test_repository_list_names_the_release_gates(self):
        path = pathlib.Path(__file__).resolve().parents[2] / ".github" / "required-checks.txt"
        required = checks.read_required(path)
        for name in ["Unit + Robolectric tests, coverage gate", "Screenshot verification (Roborazzi)", "API 33",
                     "Wear OS 34", "Unsigned release build, SBOM, changelog preview, reproducibility",
                     "Macrobenchmark (nightly)"]:
            self.assertIn(name, required)

    def test_every_required_name_is_a_job_name_in_the_workflows(self):
        root = pathlib.Path(__file__).resolve().parents[2]
        workflows = "\n".join(p.read_text(encoding="utf-8") for p in (root / ".github" / "workflows").glob("*.yml"))
        for name in checks.read_required(root / ".github" / "required-checks.txt"):
            probe = "API ${{ matrix.api-level }}" if name.startswith("API ") else name
            with self.subTest(name=name):
                self.assertIn(f"name: {probe}", workflows)


if __name__ == "__main__":
    unittest.main()
