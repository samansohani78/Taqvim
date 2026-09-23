# ADR-0043: A day-list presentation for the month page above the grid's font-scale cap

- **Status:** Accepted
- **Date:** 2026-09-23
- **Plan reference:** docs/PLAN.md T-1701 (RTL & font scale: no clipping at font scale 2.0), T-701 (month grid),
  T-801 (month pager)
- **Review reference:** docs/reviews/2026-09-19-adversarial-review.md R10 ("Large-font setting is deliberately
  overridden")

## Context

`MonthGrid` lays out a month as a fixed 7-column grid: every cell gets the same width and height from the page's
measured size, and BUG-2's fit probe proves once per page that the day numbers and secondary dates fit that cell at
some scale, so cells draw without a per-cell auto-size search. That proof only holds up to a limit — `DayCell.kt` and
`MonthGrid.kt` cap the density passed to a cell at `MAX_CELL_FONT_SCALE = 1.3f` (`DayCell.kt:91`,
`MonthGrid.kt:244`), so a phone-width grid keeps working at any system font scale, but a user who asks for 2.0 in
Android's display settings sees calendar text no larger than 1.3 would give.

R10 found this a deliberate but insufficient product choice: `docs/PROVENANCE.md` recorded the cap as intentional,
but PLAN's T-1701 requires no clipping at font scale 2.0, not a capped substitute — and a screenshot named `fs200`
passed only because it captured the capped, not the requested, size. A fixed 7-column grid cannot be reflowed to fit
2.0× text on a phone-width window without either clipping or capping; the two are the only options a grid alone
offers.

## Decision

1. **Two presentations, one model.** `MonthGrid` is unchanged: at or below `MAX_CELL_FONT_SCALE` it keeps drawing
   exactly as before, including the one-node cell rendering of BUG-2 (`main@e8c03cb`). A new `MonthDayList`
   (`core/ui/component/MonthDayList.kt`) draws the same `MonthGridModel` as a scrollable, full-width list — one row
   per day — at the caller's real, uncapped density. Both take the same model and the same `onDayClick`/
   `onDayLongClick`/`onWeekClick` callbacks, so a caller only has to choose which one to compose; neither component
   makes that choice for itself.
2. **The threshold is the existing cap.** `MonthDisplayMode.forFontScale` (`core/ui/component/MonthDisplayMode.kt`)
   returns `GRID` at or below `MAX_CELL_FONT_SCALE` and `LIST` above it — the same 1.3 already used to size a grid
   cell, so there is one number, not two independent ones that could drift apart.
3. **The caller switches, not the component.** `MonthPageView` (`feature/calendar/MonthPager.kt`) reads
   `LocalDensity.current.fontScale` and picks `MonthGrid` or `MonthDayList` accordingly. `:core:ui` stays a library of
   two interchangeable presentations; the feature module owns the product decision of which one the month page shows.
   The month-grid galleries in `:core:ui`'s own component-screenshot tests keep drawing `MonthGrid` directly (a
   component sample, not the app's month page), so they are unaffected by this switch.
4. **The list row shows what the cell shows.** A row draws the day's `cellTexts` (day number, then secondary dates,
   then a shift label — the same order, styles and colors `DayCell` uses), the event-indicator dots, the selected
   fill and the today ring, and carries the day's existing spoken `contentDescription` and selection state. Nothing
   here caps the font scale, and no line sets `maxLines`, so a label that does not fit its row's width wraps instead
   of being clipped or ellipsized (T-1701's `LayoutAudit`). A row is at least 48 dp tall, meeting the touch-target
   size R10 also flagged as unverified for the grid's narrower 40 dp cells (the grid's minimum cell height is
   unchanged by this ADR; it is a separate, pre-existing choice this ADR does not revisit).
5. **The week-number shortcut survives the switch.** `MonthGrid`'s optional week-number column opens the timeline on
   tap. `MonthDayList` shows the same week number as a header row above its seven days, with the same spoken form and
   the same `onWeekClick` action, so the shortcut stays reachable at every font scale rather than being dropped when
   the grid is replaced.

## Consequences

- Below and at font scale 1.3 nothing changes: same component, same performance characteristics, same screenshots.
- Above 1.3 the month page is a list, not a grid: a returning user who increases their font scale past the cap sees a
  different navigation shape (vertical scroll through the month instead of a 6-week grid), which is the intended
  trade-off — R10 rejected capping the requested size as the alternative.
- `MonthDayList` is not virtualization-optimized the way `MonthGrid`'s BUG-2 work is: a month page is at most 42 rows,
  which a `LazyColumn` composes cheaply, and this presentation is only shown to the minority of sessions above the
  1.3 cap, so the grid's one-node-per-cell optimization was not ported here.
- `feature/year`'s year overview and `feature/widgets`' home-screen widget keep calling `MonthGrid` directly at every
  font scale (out of this review finding's scope, R10's location list); they are candidates for the same treatment if
  a future finding raises them.
- Screenshots of the calendar's month page and adaptive layout at font scale 2.0 (`calendar_month_en_01`,
  `calendar_month_fa_01`, `calendar_adaptive`, `..._fs200_*`) now record the list, not a capped grid; the reduced
  large-text screenshot matrix (`ScreenshotMatrix.largeText()`, fa RTL and en LTR) still covers exactly the two
  environments the module always has.
