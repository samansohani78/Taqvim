# ADR-0020: Dataset updates ship with app releases

- **Status:** Accepted
- **Date:** 2026-09-14
- **Plan reference:** docs/PLAN.md T-1901 (data-only release path: bundled override + signed dataset update file loader),
  T-1900 (release engineering)

## Context

The plan describes a way to ship holiday and event data corrections without an app release: a signed dataset update
file downloaded and verified by the app. That design needs a signing-key custody process and, below Android 13, a new
cryptography dependency for Ed25519 verification. An unsigned HTTPS download would trust the hosting server alone.

## Decision

Chosen by the owner on 2026-09-14:

- Dataset corrections ship **inside normal app releases** only. The dataset is compiled at build time (D-08) as before.
- No signed dataset update loader and no network dataset fetch are built for now.
- The only network use stays the optional iCalendar subscriptions (T-1003, ADR-0017).

## Consequences

- No signing key to manage and no extra network surface or dependency.
- A data error is fixed by a normal (possibly expedited) release. SUPPORT.md's 72-hour data-error target therefore
  depends on how fast a release can be published and adopted.
- The loader can still be added later behind a new ADR; the dataset format does not need to change for that.
