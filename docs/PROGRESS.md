# Taqvim — Progress Tracker

Source of truth: [`docs/PLAN.md`](PLAN.md). Deviations: [`docs/adr/`](adr/).
Status legend: `TODO` · `WIP` · `DONE` · `BLOCKED` (see Notes).

| Task | Title | Status | Branch/Commit | Tests added | Notes |
|---|---|---|---|---|---|
| T-000 | Repository bootstrap | TODO | | | |
| T-001 | License gate | TODO | | | |
| T-002 | Architecture rules (Konsist) | TODO | | | |
| T-003 | CI workflows | TODO | | | |
| T-004 | Custom lint module | TODO | | | |
| T-005 | Test infrastructure (`:core:testing`) | TODO | | | |
| T-100 | Value types | TODO | | | |
| T-101 | Gregorian ↔ JDN (A-01) | TODO | | | |
| T-102 | Persian calendar (A-02) | TODO | | | |
| T-103 | Islamic tabular (A-03) & UAQ (A-04) | TODO | | | |
| T-104 | Islamic Iran official (A-05) + observational (A-06) | TODO | | | |
| T-105 | Nepali (A-07) | TODO | | | |
| T-106 | Calendar utilities | TODO | | | |
| T-107 | `kotlinx.datetime` bridge | TODO | | | |
| T-200 | Language table | TODO | | | |
| T-201 | Numerals | TODO | | | |
| T-202 | Date & duration formatting | TODO | | | |
| T-203 | Persian text normalization & fuzzy match | TODO | | | |
| T-204 | String resources plan | TODO | | | |
| T-300 | Rule engine | TODO | | | |
| T-301 | Year cache & day lookup | TODO | | | |
| T-302 | Policies | TODO | | | |
| T-303 | Holiday determination & workday basics | TODO | | | |
| T-304 | Search index | TODO | | | |
| T-305 | Repository impl (`:data:events`) | TODO | | | |
| D-01 | Dataset JSON Schema + validator CLI | TODO | | | |
| D-02 | Iran official holidays 1403–1406 | TODO | | | |
| D-03 | Afghanistan set | TODO | | | |
| D-04 | Nepal set | TODO | | | |
| D-05 | UN international days | TODO | | | |
| D-06 | Ancient Iranian festivals | TODO | | | |
| D-07 | Islamic Iran override table 1390–1410 | TODO | | | |
| D-08 | Dataset code generator (KotlinPoet) | TODO | | | |
| D-09 | Dataset CI + CONTRIBUTING-DATA.md | TODO | | | |
| T-400 | Solar position (A-09) | TODO | | | |
| T-401 | Prayer times (A-10) | TODO | | | |
| T-402 | Qibla & great-circle (A-11) | TODO | | | |
| T-403 | Astronomy façade (A-13) | TODO | | | |
| T-404 | Zodiac & moon-in-Scorpio | TODO | | | |
| T-405 | Houses, lots, ascendant (A-14) | TODO | | | |
| T-406 | Tithi, planetary hours, Chinese/animal year, year names | TODO | | | |
| T-407 | Photography panel calculations (F-10) | TODO | | | |
| T-500 | NLP date parser (F-03) | TODO | | | |
| T-501 | Text date detector (F-04) | TODO | | | |
| T-502 | ICS reader/writer (F-05) | TODO | | | |
| T-503 | Recurrence engine | TODO | | | |
| T-504 | Workday engine (F-07) | TODO | | | |
| T-600 | Proto DataStore | TODO | | | |
| T-601 | Room schema & DAOs | TODO | | | |
| T-602 | Device calendar adapter | TODO | | | |
| T-603 | Location data | TODO | | | |
| T-604 | Scheduler | TODO | | | |
| T-605 | Backup/restore (F-11) | TODO | | | |
| T-700 | Theme | TODO | | | |
| T-701 | Components | TODO | | | |
| T-702 | Painters for widgets | TODO | | | |
| T-703 | Motion & shared elements | TODO | | | |
| T-800 | CalendarViewModel & use cases | TODO | | | |
| T-801 | Month pager | TODO | | | |
| T-802 | Day details tabs | TODO | | | |
| T-803 | Toolbar & menu | TODO | | | |
| T-804 | Search screen (F-12) | TODO | | | |
| T-805 | Year view | TODO | | | |
| T-806 | Adaptive layout (F-13) | TODO | | | |
| T-900 | Timeline layout | TODO | | | |
| T-901 | Month list & agenda | TODO | | | |
| T-1000 | Events editor | TODO | | | |
| T-1001 | Reminder notifications | TODO | | | |
| T-1002 | Official-event reminders | TODO | | | |
| T-1003 | ICS import/export & subscriptions | TODO | | | |
| T-1100 | Times tab & report | TODO | | | |
| T-1101 | Athan settings | TODO | | | |
| T-1102 | Athan playback | TODO | | | |
| T-1103 | Automation broadcasts (F-14) | TODO | | | |
| T-1200 | Widget framework | TODO | | | |
| T-1201 | Widget: 1×1 date | TODO | | | |
| T-1202 | Widget: 4×1 date + clock | TODO | | | |
| T-1203 | Widget: 2×2 date + events + next prayer | TODO | | | |
| T-1204 | Widget: 4×2 with prayer strip | TODO | | | |
| T-1205 | Widget: Month interactive | TODO | | | |
| T-1206 | Widget: Month bitmap | TODO | | | |
| T-1207 | Widget: Week strip | TODO | | | |
| T-1208 | Widget: Schedule list | TODO | | | |
| T-1209 | Widget: Sun arc | TODO | | | |
| T-1210 | Widget: Moon | TODO | | | |
| T-1211 | Widget: Map | TODO | | | |
| T-1212 | Widget: Countdown/age (F-09) | TODO | | | |
| T-1213 | Persistent notification | TODO | | | |
| T-1214 | Dynamic launcher icon | TODO | | | |
| T-1215 | QS tile, shortcuts, live wallpaper, daydream | TODO | | | |
| T-1300 | Astronomy screen | TODO | | | |
| T-1301 | Map | TODO | | | |
| T-1302 | Compass | TODO | | | |
| T-1303 | Bubble level | TODO | | | |
| T-1400 | Converter, distance, duration, time zones, QR | TODO | | | |
| T-1500 | Settings screens | TODO | | | |
| T-1501 | Language & first-run | TODO | | | |
| T-1502 | Location settings | TODO | | | |
| T-1503 | Backup/restore UI & privacy dashboard | TODO | | | |
| T-1504 | About, licenses, diagnostics & report | TODO | | | |
| T-1600 | Wear app | TODO | | | |
| T-1700 | TalkBack pass | TODO | | | |
| T-1701 | RTL & font scale | TODO | | | |
| T-1702 | Translations | TODO | | | |
| T-1800 | Baseline/startup profiles, R8, shrinking | TODO | | | |
| T-1801 | Macrobenchmarks | TODO | | | |
| T-1802 | Compose stability | TODO | | | |
| T-1803 | Memory & leaks | TODO | | | |
| T-1804 | Security | TODO | | | |
| T-1900 | Release engineering | TODO | | | |
| T-1901 | Support system | TODO | | | |
| T-1902 | Beta program | TODO | | | |

## Epic summaries

_None yet._
