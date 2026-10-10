#!/usr/bin/env python3
"""Decides whether a benchmark run qualifies a commit for release, and fails closed when it does not.

`benchmark.yml` reports two different facts. "Macrobenchmark (nightly)" says the job ran. This says the run
measured the commit — or that someone accepted in writing that it did not.

The gate exits 0 on an inconclusive comparison, deliberately, so a runner that cannot measure does not block a
release. That made the check conclude `success` on commits nothing had measured: v1.0.0, v1.1.0 and v1.1.1 each
qualified that way.

The rules, which are the whole point of this file being a file rather than four lines of shell:

1. A run that measured the commit qualifies it. Nothing else is consulted.
2. An inconclusive run qualifies the commit only if `benchmark/UNBENCHMARKED.md` waives it, by heading:
   - `### <40-hex sha>` waives exactly that commit, wherever the run happened.
   - `### <tag>` waives a release, and is read **only** on a run whose ref is that very tag, whose resolved commit
     is the commit being judged. A branch run never consumes a tag waiver, and a tag waiver never speaks for a
     commit the tag does not point at.
3. A waiver must say who accepted it and why. One that does not is not a waiver.
4. Anything else fails.

Rule 2's tag half exists because a waiver written for a release changes the very SHA it is trying to name: the
waiver has to be committed, and committing it moves the commit. A tag is decided before it is pushed. Rule 2 also
says "by heading" on purpose — a SHA mentioned in another entry's prose used to be enough to authorize a release,
because the old check was `grep -q "$GITHUB_SHA"` over the whole file.

Usage:
  check_benchmark_waiver.py --waiver benchmark/UNBENCHMARKED.md --sha <sha> \
      --ref-name <branch-or-tag> --ref-type <branch|tag> [--tag-sha <sha>] [--inconclusive true|false]

Exit code 0 when the commit qualifies, 1 when it does not.
"""
import argparse
import re
import sys

SHA_PATTERN = re.compile(r"^[0-9a-f]{40}$")
HEADING = re.compile(r"^###\s+(\S+)\s*$")
WAIVED_SECTION = re.compile(r"^##\s+Waived commits\s*$")
ACCEPTED_BY = re.compile(r"^Accepted by:\s*\S+", re.MULTILINE)
WHY = re.compile(r"^Why:\s*\S+", re.MULTILINE)


class Waiver:
    """One `### <name>` entry with the body that follows it."""

    def __init__(self, name, body):
        self.name = name
        self.body = body

    @property
    def names_a_commit(self):
        return bool(SHA_PATTERN.match(self.name))

    @property
    def is_complete(self):
        """A waiver has to say who accepted it and why; the template's own commented-out example does not."""
        return bool(ACCEPTED_BY.search(self.body)) and bool(WHY.search(self.body))


def parse_waivers(text):
    """The entries under "## Waived commits", in order. Headings before that section are prose, not waivers."""
    waivers, name, body, in_section = [], None, [], False
    for line in text.splitlines():
        if WAIVED_SECTION.match(line):
            in_section = True
            continue
        if not in_section:
            continue
        # Any other `## ` heading ends the section.
        if line.startswith("## "):
            break
        heading = HEADING.match(line)
        if heading:
            if name is not None:
                waivers.append(Waiver(name, "\n".join(body)))
            name, body = heading.group(1), []
        elif name is not None:
            body.append(line)
    if name is not None:
        waivers.append(Waiver(name, "\n".join(body)))
    # The template keeps a commented-out example; it is inside an HTML comment and never complete.
    return [waiver for waiver in waivers if not waiver.name.startswith("<")]


def decide(text, sha, ref_name, ref_type, tag_sha, inconclusive):
    """(qualified, reason) for one run. Fails closed: every path that is not a rule above returns False."""
    if not inconclusive:
        return True, "the benchmarks measured this commit and stayed inside their budgets"

    waivers = parse_waivers(text or "")
    for waiver in waivers:
        if waiver.names_a_commit:
            if waiver.name != sha:
                continue
            if not waiver.is_complete:
                return False, f"the waiver for {sha} does not say who accepted it and why"
            return True, f"released unbenchmarked on purpose: a waiver names commit {sha}"

    for waiver in waivers:
        if waiver.names_a_commit or waiver.name != ref_name:
            continue
        if ref_type != "tag":
            return False, (
                f"a waiver names the release {waiver.name}, but this run is on {ref_type} {ref_name}. "
                "A tag waiver is only read on a run of that tag."
            )
        if tag_sha and tag_sha != sha:
            return False, (
                f"a waiver names the release {waiver.name}, but that tag points at {tag_sha}, "
                f"not at the commit being judged ({sha})."
            )
        if not waiver.is_complete:
            return False, f"the waiver for {waiver.name} does not say who accepted it and why"
        return True, f"released unbenchmarked on purpose: a waiver names the release {waiver.name}"

    return False, (
        f"the benchmark run was inconclusive, so nothing measured {sha}. Re-run on a healthy runner, or accept it "
        f"in writing by adding `### {sha}`, or the tag being released, to benchmark/UNBENCHMARKED.md with who "
        "accepted it and why."
    )


def as_bool(value):
    return str(value).strip().lower() in {"true", "1", "yes"}


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--waiver", required=True)
    parser.add_argument("--sha", required=True)
    parser.add_argument("--ref-name", default="")
    parser.add_argument("--ref-type", default="")
    parser.add_argument("--tag-sha", default="")
    parser.add_argument("--inconclusive", default="false")
    args = parser.parse_args(argv[1:])

    try:
        with open(args.waiver, encoding="utf-8") as handle:
            text = handle.read()
    except OSError:
        text = ""

    qualified, reason = decide(
        text=text,
        sha=args.sha,
        ref_name=args.ref_name,
        ref_type=args.ref_type,
        tag_sha=args.tag_sha,
        inconclusive=as_bool(args.inconclusive),
    )
    print(reason)
    return 0 if qualified else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
