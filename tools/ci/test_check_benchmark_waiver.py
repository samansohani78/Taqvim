"""Tests for check_benchmark_waiver.py: a waiver authorizes one commit, and nothing else does."""
import unittest

import check_benchmark_waiver as waiver

SHA = "a" * 40
OTHER_SHA = "b" * 40

COMPLETE = """
# Commits released without a performance measurement

Prose above the section, including a stray `### {sha}` heading that is not a waiver.

### {sha}

## Waived commits

### v1.2.0
Accepted by: Someone, 2026-10-10
Why: the hosted runner cannot measure this app.

### {other}
Accepted by: Someone, 2026-10-10
Why: measured on a physical device instead.
""".strip().format(sha=SHA, other=OTHER_SHA)

NO_REASON = """
## Waived commits

### v9.9.9
""".strip()


class DecideTest(unittest.TestCase):
    def decide(self, text=COMPLETE, sha=SHA, ref_name="main", ref_type="branch", tag_sha="", inconclusive=True):
        return waiver.decide(text, sha, ref_name, ref_type, tag_sha, inconclusive)

    def test_a_measured_run_qualifies_without_any_waiver(self):
        qualified, reason = self.decide(text="", inconclusive=False)
        self.assertTrue(qualified)
        self.assertIn("measured", reason)

    def test_a_measured_run_that_regressed_is_not_this_check(self):
        # The gate itself fails on a measured regression before this runs; "not inconclusive" means it passed.
        self.assertTrue(self.decide(inconclusive=False)[0])

    def test_inconclusive_without_a_waiver_fails(self):
        qualified, reason = self.decide(text="## Waived commits\n")
        self.assertFalse(qualified)
        self.assertIn("inconclusive", reason)

    def test_an_exact_sha_waiver_qualifies_that_commit_on_a_branch_run(self):
        self.assertTrue(self.decide(sha=OTHER_SHA)[0])

    def test_an_exact_sha_waiver_does_not_qualify_another_commit(self):
        self.assertFalse(self.decide(sha="c" * 40)[0])

    def test_a_sha_in_prose_above_the_section_is_not_a_waiver(self):
        # The old check was `grep -q "$GITHUB_SHA"` over the whole file, so any mention authorized a release.
        self.assertFalse(self.decide(sha=SHA)[0])

    def test_a_tag_waiver_is_not_read_on_a_branch_run(self):
        qualified, reason = self.decide(ref_name="v1.2.0", ref_type="branch")
        self.assertFalse(qualified)
        self.assertIn("only read on a run of that tag", reason)

    def test_a_tag_waiver_qualifies_on_a_run_of_that_tag(self):
        qualified, reason = self.decide(ref_name="v1.2.0", ref_type="tag")
        self.assertTrue(qualified)
        self.assertIn("v1.2.0", reason)

    def test_a_tag_waiver_requires_the_tag_to_point_at_the_commit_being_judged(self):
        qualified, reason = self.decide(ref_name="v1.2.0", ref_type="tag", tag_sha=OTHER_SHA, sha=SHA)
        self.assertFalse(qualified)
        self.assertIn("not at the commit being judged", reason)

    def test_a_tag_waiver_passes_when_the_tag_resolves_to_the_commit(self):
        self.assertTrue(self.decide(ref_name="v1.2.0", ref_type="tag", tag_sha=SHA, sha=SHA)[0])

    def test_an_unrelated_tag_cannot_authorize_the_commit(self):
        self.assertFalse(self.decide(ref_name="v9.0.0", ref_type="tag", tag_sha=SHA)[0])

    def test_a_waiver_without_who_and_why_is_not_a_waiver(self):
        qualified, reason = self.decide(text=NO_REASON, ref_name="v9.9.9", ref_type="tag")
        self.assertFalse(qualified)
        self.assertIn("who accepted it and why", reason)

    def test_a_missing_waiver_file_fails_closed(self):
        self.assertFalse(self.decide(text="")[0])


class ParseTest(unittest.TestCase):
    def test_entries_are_read_only_from_the_waived_section(self):
        names = [entry.name for entry in waiver.parse_waivers(COMPLETE)]
        self.assertEqual(names, ["v1.2.0", OTHER_SHA])

    def test_the_commented_out_template_entry_is_ignored(self):
        text = "## Waived commits\n\n<!--\n### <full 40-character SHA, or the tag>\nAccepted by: x\nWhy: y\n-->\n"
        self.assertEqual(waiver.parse_waivers(text), [])

    def test_a_following_section_ends_the_waivers(self):
        text = "## Waived commits\n\n### v1.0.0\nAccepted by: a\nWhy: b\n\n## Notes\n\n### v2.0.0\nAccepted by: a\nWhy: b\n"
        self.assertEqual([entry.name for entry in waiver.parse_waivers(text)], ["v1.0.0"])


class BoolTest(unittest.TestCase):
    def test_truthy_spellings(self):
        for value in ["true", "True", "1", "yes", " TRUE "]:
            self.assertTrue(waiver.as_bool(value), value)

    def test_everything_else_is_false(self):
        for value in ["false", "", "0", "no", "maybe", None]:
            self.assertFalse(waiver.as_bool(value), value)


if __name__ == "__main__":
    unittest.main()
