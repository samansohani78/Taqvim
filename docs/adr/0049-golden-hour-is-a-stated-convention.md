# ADR-0049: The golden hour is a stated convention (−4°‥+6°), not a sourced quantity

- **Status:** Accepted
- **Date:** 2026-09-26
- **Plan reference:** docs/PLAN.md §6 T-407 (F-10); docs/DATA_TODO.md DT-017; ADR-0042, ADR-0047

## Context

`PhotographyPanel` (T-407) shows two bands of light. ADR-0047 closed the lower one: the blue hour's bottom edge is
civil twilight, the USNO and every other almanac define that at a **geometric** −6°, and `UsnoTwilightTest` now
asserts the app agrees with the USNO tables to 0 or −1 minutes across 120 comparisons.

The golden hour's two edges — an apparent −4° and +6° — were left open, because ADR-0047 could find nothing to
check them against. DT-017 has asked for "a published photographers' ephemeris or observatory twilight table with
stated altitude thresholds" since the row was written, and the previous pass recorded a negative result without
enumerating what it had tried. This pass enumerated it.

**Official astronomy.** The *Explanatory Supplement to the Astronomical Almanac* (USNO/HMNAO) was read in full text.
Its glossary defines exactly three named low-sun bands and no others — "civil twilight comprises the interval when
the zenith distance, referred to the center of the Earth, of the central point of the Sun's disk is between 90°50′
and 96°, nautical twilight comprises the interval from 96° to 102°, astronomical twilight comprises the interval
from 102° to 108°" — and §9.341 restates them as altitudes of −6°, −12° and −18°. The strings "golden hour" and
"magic hour" do not occur anywhere in the volume; its only "golden" is the Golden Number of the Metonic cycle. The
USNO's own data product agrees: every response in this repository's archived
`docs/sources/usno/twilight-2026-raw.json` names only `Rise`, `Set`, `Upper Transit`, `Begin Civil Twilight` and
`End Civil Twilight`. (aa.usno.navy.mil itself was unreachable from the build network this pass — every request
fails the TLS handshake with `wrong version number` — so the archived responses and the Supplement stood in for it.
HMNAO's WEBSURF is likewise unreachable, its certificate chain failing verification; the office's public
data page, which was reachable, describes its information sheets without naming any band beyond those twilights.)

**Illumination and colour standards.** The CIE's own International Lighting Vocabulary (CIE S 017, the e-ILV) has no
entry for "golden hour" or "magic hour"; it defines daylight, sunlight, skylight, daylight illuminant and daylight
locus, none of them bounded by a solar altitude. CIE S 011 / ISO 15469 (*CIE standard general sky*) takes solar
altitude as a **continuous parameter** of its luminance distributions, not as a boundary between named regimes. The
CIE D-series illuminants (D50/D55/D65/D75) are chromaticities, with no solar geometry attached at all.

**Atmospheric-optics literature.** The peer-reviewed work that relates low-sun geometry to colour — Lee and
Hernández-Andrés, "Measuring and modeling twilight's purple light" (*Applied Optics* 42, 445, 2003), Lee, "Twilight
and daytime colors of the clear sky" (*Applied Optics* 33, 4629, 1994), Spitschan et al., "Variation of outdoor
illumination as a function of solar elevation and light pollution" (*Scientific Reports* 6, 26756, 2016) — measures
chromaticity and illuminance as smooth functions of solar elevation. None of it names a threshold at which warm
light begins or ends; the reddening of the direct beam is monotonic in air mass, with no knee to standardise on.

**Meteorology and aviation.** The US National Weather Service glossary has no "golden hour" (its only light-related
G entries are *gamma ray* and *glory*). US federal aviation uses civil twilight, −6°, as the day/night boundary.
Neither −4° nor +6° appears as a defined threshold in either.

**Photographers' ephemerides — where the numbers do appear, and where they disagree.** PhotoPills' published guide
states the app's two numbers exactly: "Golden hour. Elevation between 6º and -4º. Blue hour. Elevation between -4º
and -6º", and in the same breath says why this is not a source in DT-017's sense: "Magic hours are a more diffuse
concept than twilights **because there is no mathematical definition**." The Photographer's Ephemeris uses +6° for
the upper edge but runs the lower edge down only to sunrise/sunset, not to −4°. timeanddate.com publishes −6°‥+6°
and states that the golden hour, "because it is a colloquial term, doesn't have an official definition similar to
dawn, dusk, and the 3 phases of twilight".

Measured over the 120 bands of `golden/usno/twilight-2026.csv` (5 cities × 12 months × morning and evening), those
three published conventions are not small variations of one another. The app's band runs a median of 57 minutes
(50–94); The Photographer's Ephemeris' runs 39 (35–68); timeanddate's runs 68 (60–112). One degree at the lower edge
is worth 4.7 to 10.3 minutes of clock time (median 5.4), so the published conventions sit up to ~17 minutes apart at
Berlin in December. There is no definition here to be within ±3 minutes of; there is a disagreement to pick a side
of.

## Decision

1. **The app's golden hour is the interval in which the Sun's apparent altitude is between −4° and +6°**, as
   `PhotographyPanel.BLUE_HOUR_TOP` and `GOLDEN_HOUR_TOP` already state it. Nothing about the computation changes.
2. **These two numbers are this project's own convention, and are documented as such.** No astronomical authority,
   no illumination or colorimetry standard, and no meteorological or aviation authority defines the term at all, so
   there is nothing for DT-017 to fetch. This is the same shape of decision ADR-0042 made for a missing Persian
   title and ADR-0047 made for the blue hour's basis: where no source can exist, the project states its own rule
   rather than leaving the row open forever.
3. **Why these two numbers, honestly.** They entered the project through docs/PLAN.md T-407 ("sun altitude
   −4°..6°"), which states them without a reason, and docs/PROVENANCE.md records only that the implementation "uses
   the plan's apparent solar altitude bands". The code had no stated justification. This ADR supplies one rather
   than inventing a physical derivation the numbers do not have:
   - **+6° mirrors civil twilight.** The one edge of this band with an authority behind it is −6°, and taking +6°
     as the top makes the two magic hours together tile the symmetric interval −6°‥+6° about the horizon. The
     geometry is arbitrary but it is *one* arbitrary choice, not two.
   - **−4° is the handover, not a second threshold.** It is the single number that splits −6°‥+6° into a warm band
     and a blue band; `PhotographyPanel` uses it for both, so the two bands meet exactly and never overlap or gap.
   - **Both sit inside the range where the published illumination model itself changes regime.** The
     *Explanatory Supplement*'s piecewise fit for ground illuminance from the Sun (Table 9.34.1, after the RCA
     *Electro-Optics Handbook*) changes cubic at altitudes +20°, +5°, −0.8°, −5°, −12° and −18°. The app's edges are
     within about a degree of the +5° and −5° knots, and the band they cut is within a minute of the one those knots
     would cut: over the same 120 comparisons a −5°‥+5° band has a median length of 57 minutes against the app's 57
     (50–93 against 50–94). So the convention is not merely defensible, it is numerically indistinguishable from the
     nearest thing to a published physical segmentation — while remaining a convention, because that table is a
     curve fit, not a definition of anything called a golden hour.
   - **It is the convention a reader is most likely to already hold.** Of the three published photographers'
     ephemerides checked, PhotoPills states these exact two numbers.
4. **The edges stay apparent altitudes**, for the reason ADR-0047 gave: they describe what a photographer sees, and
   they have no geometric definition to match.
5. **What the app does not claim.** The app does not claim that its golden-hour times agree with any external
   ephemeris, table or app to any tolerance, and no golden test asserts one. T-407's "±3 min against published
   data" acceptance criterion is met by the quantities that *are* defined — sunrise, sunset, transit and the blue
   hour's civil-twilight edge, all checked in `UsnoTwilightTest` — and does not extend to the golden hour, which has
   no published counterpart to be three minutes away from.
6. **No user-facing text may claim otherwise.** `PhotographyPanel` is not wired to any screen yet: there is no
   golden-hour string in any `res/values*/strings.xml`, in any of the 24 languages, so nothing over-claims today and
   nothing needs fixing. When F-10's UI lands it must describe the band as the app's own light band — not as a
   published, official or standard time — and must not quote an accuracy for it.

## Consequences

- DT-017 closes **by policy**, like DT-019 under ADR-0042: its blue-hour half was closed by ADR-0047 against the
  USNO golden, and its golden-hour half is answered here with the finding that the source it asks for does not
  exist. A future source would have to be a *new definition* published by an authority, not a table we failed to
  find; if one ever appears and differs, `Sky.altitudes` already exposes the geometric altitude ADR-0047 added, so
  moving an edge is a constant change.
- `PhotographyPanelTest` gains a case that pins the convention the way `UsnoTwilightTest` pins the blue hour's: it
  asserts the apparent altitude at each golden-hour edge is −4° and +6°, and that the golden and blue bands meet at
  the same instant. An edge that silently drifted, or a band that stopped tiling, now fails a test instead of
  quietly changing what the app means.
- The constants keep their values, so no displayed time moves and no golden fixture changes.
- Anyone reading `PhotographyPanel` is told, in its KDoc, that one of its three edges is sourced and two are
  conventional — the same asymmetry ADR-0047 introduced, now written down on both halves instead of one.
