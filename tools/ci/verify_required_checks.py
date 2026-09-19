#!/usr/bin/env python3
"""Fails unless every required check run concluded `success` on one exact commit (REVIEW R06).

release.yml runs this before it builds anything, so a tag can only produce release artifacts for a commit on which the
PR, instrumented, release dry run and benchmark jobs all passed. It fails closed: a required check that is missing,
still queued or running, skipped, neutral, cancelled, timed out or failed is a failure. When a job ran more than once
on the commit (a re-run), the most recent run of it decides.

Usage: verify_required_checks.py <owner/repo> <sha> <required-checks-file>
Reads GITHUB_TOKEN from the environment. Exit code 0 when all pass, 1 otherwise.
"""
import json
import os
import sys
import urllib.request

API = "https://api.github.com"
PAGE_SIZE = 100


def read_required(path):
    """The required check names in the file, ignoring blank lines and `#` comments."""
    with open(path, encoding="utf-8") as handle:
        lines = (line.strip() for line in handle)
        return [line for line in lines if line and not line.startswith("#")]


def latest_by_name(check_runs):
    """{name: the most recent check run of that name}, by run id (ids grow with creation time)."""
    latest = {}
    for run in check_runs:
        current = latest.get(run["name"])
        if current is None or run["id"] > current["id"]:
            latest[run["name"]] = run
    return latest


def verdicts(required, check_runs):
    """[(name, ok, reason)] for every required check, in the order of the required list."""
    latest = latest_by_name(check_runs)
    result = []
    for name in required:
        run = latest.get(name)
        if run is None:
            result.append((name, False, "missing: no run of this check on the commit"))
        elif run.get("status") != "completed":
            result.append((name, False, f"not finished: status {run.get('status')}"))
        elif run.get("conclusion") != "success":
            result.append((name, False, f"concluded {run.get('conclusion')}"))
        else:
            result.append((name, True, "success"))
    return result


def fetch_check_runs(repo, sha, token):
    """Every check run on the commit, following the API's pagination."""
    runs, page = [], 1
    while True:
        url = f"{API}/repos/{repo}/commits/{sha}/check-runs?per_page={PAGE_SIZE}&page={page}&filter=all"
        request = urllib.request.Request(url, headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {token}",
            "X-GitHub-Api-Version": "2022-11-28",
        })
        with urllib.request.urlopen(request, timeout=30) as response:
            body = json.load(response)
        batch = body.get("check_runs", [])
        runs.extend(batch)
        if len(runs) >= body.get("total_count", 0) or not batch:
            return runs
        page += 1


def main(argv):
    if len(argv) != 4:
        print(__doc__, file=sys.stderr)
        return 2
    repo, sha, required_file = argv[1:]
    token = os.environ.get("GITHUB_TOKEN")
    if not token:
        print("GITHUB_TOKEN is not set", file=sys.stderr)
        return 2
    results = verdicts(read_required(required_file), fetch_check_runs(repo, sha, token))
    for name, ok, reason in results:
        print(f"{'PASS' if ok else 'FAIL'}  {name}: {reason}")
    failed = [name for name, ok, _ in results if not ok]
    if failed:
        print(f"{len(failed)} required check(s) did not pass on {sha}; refusing to build release artifacts.")
        return 1
    print(f"All {len(results)} required checks passed on {sha}.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
