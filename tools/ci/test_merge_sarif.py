"""Tests for merge_sarif.py."""
import json
import tempfile
import unittest
import pathlib

import merge_sarif


def run(tool, rules, results, base=None):
    body = {"tool": {"driver": {"name": tool, "rules": [{"id": r} for r in rules]}}, "results": results}
    if base:
        body["originalUriBaseIds"] = {"%SRCROOT%": {"uri": base}}
    return {"version": "2.1.0", "runs": [body]}


def result(rule, index):
    return {"ruleId": rule, "ruleIndex": index, "locations": [{"physicalLocation": {"artifactLocation": {"uri": "a.kt", "index": 0}}}]}


class MergeSarifTest(unittest.TestCase):
    def test_one_run_per_tool_with_all_results_and_unique_rules(self):
        merged = merge_sarif.merge([
            run("detekt", ["A", "B"], [result("B", 1)], "file:///repo/"),
            run("detekt", ["B", "C"], [result("C", 1)], "file:///repo/"),
            run("Android Lint", ["L"], [result("L", 0)]),
        ])
        self.assertEqual(sorted(merged), ["Android Lint", "detekt"])
        detekt = merged["detekt"]["runs"]
        self.assertEqual(len(detekt), 1)
        self.assertEqual([r["id"] for r in detekt[0]["tool"]["driver"]["rules"]], ["A", "B", "C"])
        self.assertEqual([r["ruleId"] for r in detekt[0]["results"]], ["B", "C"])
        self.assertNotIn("ruleIndex", json.dumps(detekt))
        self.assertNotIn('"index"', json.dumps(detekt))
        self.assertEqual(detekt[0]["originalUriBaseIds"]["%SRCROOT%"]["uri"], "file:///repo/")

    def test_main_writes_a_file_per_tool(self):
        with tempfile.TemporaryDirectory() as tmp:
            source = pathlib.Path(tmp, "in.sarif")
            source.write_text(json.dumps(run("Android Lint", [], [])), encoding="utf-8")
            self.assertEqual(merge_sarif.main([str(pathlib.Path(tmp, "out")), str(source)]), 0)
            self.assertTrue(pathlib.Path(tmp, "out", "android-lint.sarif").exists())

    def test_main_without_arguments_reports_usage(self):
        self.assertEqual(merge_sarif.main([]), 2)


if __name__ == "__main__":
    unittest.main()
