# ADR-0047: The blue hour's lower edge is civil twilight, measured geometrically

- **Status:** Accepted
- **Date:** 2026-09-25
- **Plan reference:** docs/PLAN.md §6 T-407 (F-10); docs/DATA_TODO.md DT-017, DT-011; ADR-0029

## Context

`PhotographyPanel` (T-407) shows two bands of light: the golden hour where the Sun is between −4° and +6°, and the
blue hour where it is between −6° and −4°. All three numbers were compared against `Sky.skyPosition`, which returns an
**apparent** altitude — where the Sun is seen, with the atmosphere bending its light — because that is what
`Refraction.Normal` gives.

The USNO golden added for DT-017/DT-011 (main@1fd5ebd: the 21st of each month of 2026 for Tehran, Kabul, Istanbul,
Berlin and Sydney) showed that the lower edge disagreed with the published tables. Civil twilight is defined at a
**geometric** −6°, the Sun's true position, and cosinekitty/astronomy clamps `Refraction.Normal` below the horizon to
roughly its horizon value, so an apparent −6° is about −6.6° geometric. Measured over the 120 comparisons in that
golden, the app's blue hour opened **2 to 7 minutes early and closed 2 to 7 minutes late**, largest at Berlin, and
never in the other direction.

That is a convention difference rather than an ephemeris error — the same golden shows rise, set and transit agreeing
within the ±3 minutes T-407 asks for — but it is a difference a user can see: the app's blue hour did not start when
every published twilight table says twilight starts.

## Decision

1. **The blue hour's lower edge is the geometric −6° that defines civil twilight.** `Sky.altitudes` returns both
   altitudes from one position computation, and `PhotographyPanel` compares `BLUE_HOUR_BOTTOM` against
   `Altitudes.geometricDegrees`.
2. **The other two edges stay apparent altitudes.** −4° and +6° have **no published definition to agree with**; a
   search for a photographers' ephemeris or observatory table that states them found none (DT-017). They are this
   app's own convention, they describe what a photographer sees, and changing their basis would move the golden hour
   by minutes for no source to check it against. So the two edges of one band are deliberately measured differently:
   the one with a published definition matches it, the ones without keep the observer's point of view.
3. **Sunrise and sunset are untouched.** They are *defined* with refraction (USNO's −0.8333° includes refraction and
   semidiameter), they come from `Sky.riseSetTransit` rather than this panel, and they must keep matching the golden.

## Consequences

- The blue hour now agrees with USNO civil twilight to **0 or −1 minutes** across all 120 comparisons, against 2–7
  minutes before. The residual is rounding: the USNO publishes whole minutes and the interval search resolves to 10
  seconds.
- The blue hour is about 35 % shorter than it was, because its lower edge moved up by ~0.57° while the upper edge did
  not. At Tehran in September it runs roughly 6 minutes rather than 8.
- `UsnoTwilightTest` no longer pins the size and direction of a known disagreement; it asserts agreement within one
  minute, and a second case asserts the *convention* — that at the blue hour's end the geometric altitude is −6° while
  the apparent altitude is measurably higher — so the edge cannot silently revert to an apparent comparison and still
  pass. Both cases fail against the previous behaviour.
- `Sky.altitudes` is public API. Anything else that needs a geometrically defined threshold (the −4° and +6° edges, if
  a source for them ever appears; the crescent and Odeh criteria already compute their own geometric altitudes) has
  one call to use instead of re-deriving refraction.
- DT-017 stays **open**: the golden-hour edges still have no published source, which no decision here can supply.
