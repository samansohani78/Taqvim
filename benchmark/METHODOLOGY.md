# What each benchmark measures

Every benchmark in this project answers a narrower question than its name suggests. This file states, per
benchmark: what starts the timer, what stops it, what is deliberately outside it, whether the number is absolute or
only comparable against a baseline, and whether it means anything on a hosted runner.

It exists because the project has twice published a figure that did not mean what it said — a Glance timing read
against a budget belonging to a different benchmark, and a difference between two separately measured runs called a
composition cost (see `UNBENCHMARKED.md` and ADR-0018's addendum). Both were honest mistakes of exactly one kind:
nobody had written down where the timer started.

## The rule about mixing

A number must not mix:

- **app-owned computation** — what Taqvim's own code does;
- **Android framework overhead** — Glance sessions, `RemoteViews` translation, Room, the Compose runtime;
- **host overhead** — the launcher inflating a widget, SurfaceFlinger compositing a frame;
- **machine noise** — scheduling, thermal state, an emulator's software renderer.

Where a benchmark unavoidably contains more than one, it says so and is not given a budget. A budget on a mixed
number cannot be met or missed for a reason anyone can act on.

## Macrobenchmarks (`:benchmark`, `androidTest`, needs a device or emulator)

| Benchmark | Timer starts | Timer ends | Excluded | Absolute? | Hosted runner |
|---|---|---|---|---|---|
| `StartupBenchmark.startupCold` | process launch, `StartupMode.COLD` | `timeToInitialDisplay` — the first frame the app produces | nothing the app does before launch; the user's own home-screen latency | yes, §9 budget 350 ms, `physicalDeviceOnly` | reports a number; the hosted ceiling is a gross-regression stop, not a budget |
| `StartupBenchmark.startupWarm` | process already alive, activity recreated | same | same | bounded by the cold budget (§9 names no warm one) | as above |
| `StartupBenchmark.startupColdWithoutProfile` | cold launch with the baseline profile suppressed | same | same | **no** — it exists only to be read against `startupCold` on the same run and device | the difference is what matters, and only on a physical arm64 device |
| `MonthPagerScrollBenchmark.scrollTwentyFourMonths` | gesture begins | 24 months scrolled | first composition, data load | `frameDurationCpuMs.P99` ≤ 16 ms, `physicalDeviceOnly` | **no jank metrics at all** — a hosted emulator has no SurfaceFlinger frame timeline, so only `frameCount` is reported |
| `ScreenScrollBenchmarks.searchTyping` | first keystroke | the typed query's results are on screen | index construction before the gesture | no budget: §9's "< 20 ms per query over 10 000 events" is a *per-query* figure and this reports frames, not queries | `frameCount` only |
| `ScreenScrollBenchmarks.timelineScroll`, `YearViewBenchmark.*`, `MapScreenBenchmark.mapPanAndZoom` | gesture begins | gesture ends | as above | no absolute budget | `frameCount` only |
| `MonthScreenMemoryBenchmark.monthScreenMemory` | journey begins | journey ends | — | **no** — see the long note in `budgets.json`: `memoryRssFile` is shared clean mapped code, and `mem.rss.anon` ratchets in these traces. §9's 80 MB is checked by hand with `dumpsys meminfo` TOTAL PSS | reported, gated only as a 10 % regression against a same-device baseline |

## Microbenchmarks (`:benchmark:micro`, in-process, no UI)

| Benchmark | Timer starts | Timer ends | Excluded | Absolute? |
|---|---|---|---|---|
| `WidgetRenderBenchmark.*` | the painter is called | the bitmap is complete | Glance, `RemoteViews`, the launcher, data loading | **yes** — this is what plan §9's 30 ms render budget applies to, and the only thing it applies to |
| `MapMaskBenchmark.*` | mask computation begins | mask complete | drawing | yes, 150 ms |
| `GlanceWidgetBenchmark.*` | `GlanceAppWidget.compose` is called | it returns `RemoteViews` | data loading (the data is fixed); the launcher's inflation | **no, and no budget** — dominated by a fixed per-call cost of roughly 405 ms that moves with neither content nor CPU |
| `GlanceWidgetBenchmark.pairedAgainstEmpty` | each side of a pair | each side of a pair | — | **no** — reports the per-pair signed difference with both sides. Its *spread* is the result: tight means the fixed cost is additive and subtraction is meaningful; wide means it is not. Until a device run says which, such a difference is an **incremental wall-clock estimate**, never a composition cost |

## Widget first useful render: what is and is not measured

"How long until a widget shows something useful" decomposes into seven stages. Four are measurable here, three are
not, and no benchmark in this project reports the whole:

| Stage | Measured by | Notes |
|---|---|---|
| 1. process start / Koin graph | `StartupBenchmark` (for the app, not the widget host) | a widget update may run in an already-live process or a cold one; which is not under the app's control |
| 2. preference load | not timed on its own | one DataStore read, shared with everything else |
| 3. calendar / date computation | not timed on its own | pure arithmetic, no I/O |
| 4. event loading, if the widget draws events | **counted, not timed**, by `WidgetDataNeedsTest` | the honest unit here is "did it touch Room / the feed cache at all", because the cost is dominated by waiting on flows, not by CPU |
| 5. Glance composition | `GlanceWidgetBenchmark`, mixed with the fixed session cost | see above |
| 6. `RemoteViews` translation | inside the same number as stage 5 | not separable from inside the process |
| 7. launcher inflation and display | **not measured, and not measurable from inside this process** | belongs to the host app |

Stage 4 is why `WidgetDataNeedsTest` counts calls rather than timing them: a widget that quietly assembles a day it
never draws looks identical on screen and identical in a wall-clock timing dominated by stage 5, but it has woken
Room, the subscription cache and their flows. Counting is the measurement that discriminates.

## Controls, and what "inconclusive" means

`required.json` lists control benchmarks: fixed synthetic drawing and mask computation that the app's screens and
data cannot influence. When a control moves more than its band (±50 %), the machine moved, and the whole comparison
is reported **inconclusive** rather than failed — timings from that machine cannot be trusted in either direction.

An inconclusive run does not block a release by itself, which is precisely why `Performance qualified` exists as a
second check: it fails unless the run measured the commit or `UNBENCHMARKED.md` names it. See `UNBENCHMARKED.md`.

## Baselines are per machine

`benchmark/baselines` holds recorded medians. A baseline is only comparable against a run on the **same kind of
machine**: a hosted x86 emulator and an arm64 phone differ by −27 % to −56 % on the same compute-bound
microbenchmarks, and in the opposite direction on others. Comparing across them produces a number with no meaning,
not a conservative one.
