# ADR-0015: App navigation model

- **Status:** Accepted
- **Date:** 2026-09-14
- **Plan reference:** docs/PLAN.md §3.1 (`:app` — NavHost, deep links), §3 stack (Navigation 3, adaptive layouts),
  F-13 (adaptive layout); deep links themselves belong to T-1103

## Context

Every feature module exposes a `…Route` composable with navigation callbacks (`CalendarNavigation`, `YearNavigation`,
`AgendaNavigation`, `TimelineNavigation`, `SearchNavigation`, `EventEditorRoute.onClose`). Features must not depend on
each other or on `:app` (Konsist), so only `:app` can connect them. The plan names Navigation 3, whose back stack is a
list owned by the app.

## Decision

1. **Destinations** are a `@Serializable` sealed interface `AppDestination : NavKey` in `:app`: one object per screen
   without arguments, `Timeline(initialDay)`, `EventEditor(eventId)` and `Pending(feature)` for planned screens that do
   not exist yet (shift work, the settings of T-1500).
2. **Back stack** (`AppBackStack`, pure and unit-tested): the calendar is always at the bottom. The four top-level tabs
   are Calendar, Times, Tools and More; selecting a tab other than the calendar leaves `[Calendar, tab]` and closes
   screens opened from the previous tab, selecting the current tab returns to its screen, and other screens are pushed
   once. Back pops one screen; with the calendar alone the system handles back and leaves the app.
3. **State:** `AppNavigator` holds the back stack and is saved with `rememberSaveable` as JSON (kotlinx.serialization),
   so it survives configuration changes and process death; unreadable saved text restores the calendar.
4. **Display:** Navigation 3 `NavDisplay` (predictive back) with the saveable-state and ViewModel-store entry decorators,
   so each screen keeps its own saved state and Koin ViewModels.
5. **Frame:** Material 3 `NavigationSuiteScaffold` — a navigation bar on compact windows, a rail on larger ones.
6. **Callbacks:** `AppRouter` maps each feature callback to a destination or platform action. Personal events open the
   editor, device events open the device calendar app, other events and day or month targets open the calendar (no
   feature accepts a target day yet). Web links open in a Custom Tab, falling back to any browser; only http(s).

## Consequences

- Deep links (T-1103) add destinations and parse them onto this back stack.
- Opening a specific day, month or planetary-hours dialog needs initial-state parameters in the calendar, year and
  astronomy routes; until then the router opens those screens at their defaults.
- The event editor has no initial date or time, so "new event on this day" and timeline drag-to-create open an empty
  editor.
