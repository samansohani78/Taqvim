"""Tests for build_review_queue.py."""
import json
import pathlib
import tempfile
import unittest

import build_review_queue


def event(**overrides):
    base = {
        "id": "test.one",
        "calendar": "GREGORIAN",
        "source": "INTERNATIONAL",
        "category": "INTERNATIONAL",
        "isHoliday": False,
        "title": {"fa": "یک", "en": "One"},
        "rule": {"type": "Fixed", "month": 1, "day": 1},
        "citations": [{"url": "https://example.org/a", "title": "t", "page": "1"}],
        "updated": "2026-10-06",
        "reviewedBy": "pending",
    }
    base.update(overrides)
    return base


def queue(*events):
    with tempfile.TemporaryDirectory() as directory:
        path = pathlib.Path(directory)
        (path / "a.json").write_text(json.dumps({"schemaVersion": 1, "events": list(events)}), encoding="utf-8")
        return build_review_queue.rows(path)


class BuildReviewQueueTest(unittest.TestCase):
    def test_a_machine_translated_title_comes_before_the_record_itself(self):
        rows = queue(event(titleReview=["fa"]))

        self.assertEqual(
            [r["state"] for r in rows],
            [build_review_queue.STATE_TITLE_MT, build_review_queue.STATE_UNREVIEWED],
        )
        self.assertEqual(rows[0]["language"], "fa")
        self.assertEqual(rows[0]["current_title"], "یک")

    def test_an_attested_record_is_last_and_carries_its_reviewer_and_date(self):
        rows = queue(event(id="test.done", reviewedBy="A. Reviewer", reviewedOn="2026-10-06"))

        self.assertEqual([r["state"] for r in rows], [build_review_queue.STATE_DONE])
        self.assertEqual(rows[0]["reviewed_by"], "A. Reviewer")
        self.assertEqual(rows[0]["reviewed_on"], "2026-10-06")

    def test_the_queue_proposes_no_titles_of_its_own(self):
        # The script lists what is waiting; inventing a "canonical" title would be the exact thing it must not do.
        rows = queue(event(titleReview=["fa"]))

        self.assertTrue(all(r["proposed_title"] == "" for r in rows))

    def test_the_schema_file_is_not_read_as_a_dataset(self):
        with tempfile.TemporaryDirectory() as directory:
            path = pathlib.Path(directory)
            (path / "events.v1.json").write_text(json.dumps({"events": [event()]}), encoding="utf-8")

            self.assertEqual(build_review_queue.rows(path), [])

    def test_the_tsv_has_one_header_and_one_line_per_row(self):
        text = build_review_queue.to_tsv(queue(event(titleReview=["fa"])))
        lines = text.strip().split("\n")

        self.assertEqual(lines[0].split("\t"), list(build_review_queue.COLUMNS))
        self.assertEqual(len(lines), 3)


if __name__ == "__main__":
    unittest.main()
