#!/usr/bin/env python3
"""Builds the human review queue of the dataset (ADR-0042, docs/REVIEWING.md).

Machine validation and human attestation are separate things. The validator in `:tools:dataset` says a record is
well formed; only a person can say its title is right. This script does not decide anything: it lists what is
waiting, in the order the owner set, so a reviewer can work through it and record what they checked.

Nothing here writes `reviewedBy` or removes a `titleReview` tag. Those change only when a reviewer has actually
read the record, and the change is theirs to make.
"""
from __future__ import annotations

import argparse
import json
import pathlib
import sys

PENDING = "pending"

#: Review states, in the order a reviewer meets them.
STATE_TITLE_MT = "title-machine-translated"
STATE_UNREVIEWED = "record-unreviewed"
STATE_DONE = "reviewed"

#: Priority of each state, lowest first (the owner's order: Persian titles, then the records, then the rest).
PRIORITY = {STATE_TITLE_MT: 0, STATE_UNREVIEWED: 1, STATE_DONE: 2}

COLUMNS = (
    "priority",
    "state",
    "id",
    "language",
    "current_title",
    "proposed_title",
    "source",
    "citation_url",
    "reviewed_by",
    "reviewed_on",
)


def rows(dataset: pathlib.Path):
    """One row per thing a person has to look at, most urgent first."""
    found = []
    for path in sorted(dataset.rglob("*.json")):
        if path.name == "events.v1.json":
            continue
        document = json.loads(path.read_text(encoding="utf-8"))
        for event in document.get("events", []) or []:
            found.extend(_rows_of(event))
    found.sort(key=lambda row: (row["priority"], row["id"], row["language"]))
    return found


def _rows_of(event: dict):
    """The rows of one record: one per machine-translated title, plus one for the record itself."""
    titles = event.get("title", {}) or {}
    citation = (event.get("citations") or [{}])[0]
    reviewer = event.get("reviewedBy", "")
    reviewed_on = event.get("reviewedOn", "")
    attested = bool(reviewer) and reviewer != PENDING
    base = {
        "id": event.get("id", ""),
        "source": event.get("source", ""),
        "citation_url": citation.get("url", ""),
        "reviewed_by": reviewer,
        "reviewed_on": reviewed_on,
    }
    for language in event.get("titleReview", []) or []:
        yield dict(
            base,
            priority=PRIORITY[STATE_TITLE_MT],
            state=STATE_TITLE_MT,
            language=language,
            current_title=titles.get(language, ""),
            # A canonical title exists only where a reviewer or a source has supplied one; the script invents none.
            proposed_title="",
        )
    state = STATE_DONE if attested else STATE_UNREVIEWED
    yield dict(
        base,
        priority=PRIORITY[state],
        state=state,
        language="",
        current_title=titles.get("fa") or titles.get("ne") or next(iter(titles.values()), ""),
        proposed_title="",
    )


def to_tsv(found) -> str:
    header = "\t".join(COLUMNS)
    lines = ["\t".join(str(row[column]).replace("\t", " ") for column in COLUMNS) for row in found]
    return "\n".join([header, *lines]) + "\n"


def summary(found) -> str:
    counts = {}
    for row in found:
        counts[row["state"]] = counts.get(row["state"], 0) + 1
    parts = [f"{counts.get(state, 0)} {state}" for state in (STATE_TITLE_MT, STATE_UNREVIEWED, STATE_DONE)]
    return f"{len(found)} rows: " + ", ".join(parts)


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("dataset", type=pathlib.Path, help="the dataset/ directory")
    parser.add_argument("output", type=pathlib.Path, nargs="?", help="where to write the TSV (default: stdout)")
    arguments = parser.parse_args(argv)

    found = rows(arguments.dataset)
    text = to_tsv(found)
    if arguments.output:
        arguments.output.write_text(text, encoding="utf-8")
        print(summary(found))
    else:
        sys.stdout.write(text)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
