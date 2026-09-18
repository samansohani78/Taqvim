# ADR-0039: CC BY 4.0 is allowed for bundled data, never for code

- **Status:** Accepted (amends ADR-0003)
- **Date:** 2026-09-18
- **Plan reference:** docs/PLAN.md §0.1, T-001, T-1301; ADR-0003, ADR-0005

## Context

ADR-0003 lists the licenses a **Maven dependency** may carry. It says nothing about **data**, and the tectonic plate
geometry of the world map (T-1301) is CC BY 4.0: Matthews et al. (2016), Zenodo record 10526157. The owner asked on
2026-09-18 to either admit CC BY through an ADR with the attribution wired into the About screen, or to replace the
data with a CC0/public-domain source.

No usable public-domain replacement exists (licence research 2026-09-15, re-checked 2026-09-18):

| Source | Licence | Verdict |
|---|---|---|
| Bird (2003) PB2002, from the author's server | none stated | unusable: no grant at all |
| USGS and `fraxen/tectonicplates` copies of PB2002 | ODC-BY 1.0 (fraxen), none (USGS page) | attribution licence as well, on data the original author never licensed |
| GEM Global Active Faults | CC BY-SA 4.0 | share-alike, forbidden |
| `dhasterok/global_tectonics` | GPL-3.0 | forbidden by the clean-room rules |
| UTIG PLATES | none stated | unusable |

Replacing CC BY with public domain is therefore not possible for this layer. CC BY 4.0 asks only for attribution
(§3(a)): the creators, a copyright notice, the licence notice and its URI, the disclaimer, a link to the material and
a note of any modifications. It imposes no share-alike and no restriction on how the app is licensed, so it does not
touch the goal of ADR-0003 — keeping a later relicensing of Taqvim itself possible.

## Decision

1. **Data is a separate allow-list.** `config/license/allowed-licenses.json` gains a `dataLicenses` section next to
   `licenses`. It covers files bundled with the app or used to validate it — never code, never a Maven coordinate.
   Each entry is an SPDX id plus `attribution` (`required` or `none`). Admitted today: `Unicode-3.0`, `CC0-1.0`,
   `LicenseRef-Public-Domain`, `LicenseRef-Cited-Publication` and `CC-BY-4.0`.
2. **CC BY 4.0 is allowed for data only.** It stays out of the dependency `licenses` section, so a CC BY *library*
   still fails `licenseCheck`; admitting one would need its own ADR.
3. **Share-alike and non-commercial stay forbidden**, for data as for code: CC BY-SA, ODbL, CC BY-NC and every
   copyleft licence. The gate's test asserts that no `-SA` or `-NC` id is in `dataLicenses`.
4. **Where attribution appears.** For every source whose licence entry says `attribution: required`:
   - **About › Data sources** shows a credit line under the source (`DataSource.attribution`) with creators, year,
     title, source link, licence name and URI, the disclaimer and what Taqvim changed, in Latin script whatever the
     app language is, plus a button that opens the licence deed (`DataLicense.deedUrl`) or the bundled licence text;
   - the **map itself** keeps the short attribution it already draws (`map_plates_attribution`), so the credit shows
     without opening About;
   - **docs/PROVENANCE.md** keeps the full reference, the archive checksum and the list of modifications.
5. **Enforcement.** `AllowListParser` parses and validates the new section (a typo in `attribution` fails the build),
   and `DataSourceLicenseTest` (`:feature:about`) checks the repository allow-list against the `DataLicense` and
   `DataSource` entries: every licence shown in the app is admitted for data, and every attribution-required source
   carries a credit line.
6. **The SBOM stays a dependency document.** `cyclonedxDirectBom` describes `releaseRuntimeClasspath`, i.e. Maven
   components. Bundled data has no coordinate and is deliberately not added to it; the register of bundled data is
   docs/PROVENANCE.md together with About › Data sources.

## Consequences

- The world map keeps the Matthews et al. (2016) geometry; no re-generation of `plates-matthews-2016.txt` is needed.
- Taqvim's own licence is unaffected: attribution is a notice obligation, not a copyleft one, and it applies to the
  data file only.
- A new bundled dataset now needs an allow-list entry before it can be shown in About, and an attribution line when
  its licence asks for one.
- Removing the CC BY layer later would also mean removing its credit lines; until then they must stay verbatim.
