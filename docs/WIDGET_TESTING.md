# Testing the widgets

How Taqvim proves its thirteen widgets work, what each layer can and cannot prove, and the bugs that taught us the
difference (T-1200).

> **A loading placeholder is a failure.** Every Taqvim widget declares
> `android:initialLayout="@layout/glance_default_loading_layout"`, so a launcher shows a spinner from the moment the
> widget is bound until Glance delivers its first `RemoteViews`. "The host received something" and "the user can
> read their date" are different facts. Only the second one is the product working, and only the second one may be
> asserted as success.

## The two layers

Neither layer may claim what only the other can prove. When a test could live in either, it lives in Layer A.

### Layer A — local, no device (`test/`, Robolectric and plain JVM)

Fast, runs on every push, and is where a regression should be caught. It covers:

| What | Where |
|---|---|
| Which data each kind reads, and which it must not | `WidgetDataNeedsTest` |
| ThreeDates reads nothing but the calendar engines | `WidgetDataNeedsTest` |
| The loader always finishes — silent store, silent source, slow source | `WidgetLoadNeverHangsTest` |
| Failure and cancellation states | `WidgetRefresherTest` |
| Which trigger refreshes which widget, and that every dependency has one | `WidgetUpdatePolicyTest` |
| Holiday colouring invalidation | `WidgetHolidayInvalidationTest` |
| Content, countdown, calendar and sky model building | `WidgetContentBuilderTest` and friends |
| Composition of each widget's Glance tree | `*GlanceTest` |
| An upgrade or reboot does not cancel scheduled work | `UpgradeKeepsScheduledWorkTest` |
| No production source cancels work wholesale | `SecurityKonsistTest` |
| Every kind's text stays inside every span it offers | `EveryWidgetFitsItsBoxTest` |
| Dates read in their own direction and are never cut short | `DateHeaderRenderingTest`, `ThreeDatesRenderingTest` |

### Layer B — real Android runtime (`androidTest/`, emulators and the owner's phone)

Slower, needs a device, and covers only what a JVM cannot fake:

| What | Where |
|---|---|
| Binding through a real `AppWidgetHost` and the `RemoteViews` the host applies | `DeviceWidgetContentTest` |
| Every one of the thirteen kinds reaching readable content | `DeviceWidgetContentTest` |
| Two instances of one kind rendering independently | `DeviceWidgetContentTest` |
| Scheduled work surviving an upgrade on a real JobScheduler | `DeviceWidgetContentTest` |
| A preference change reaching an already-bound widget | `DeviceWidgetContentTest` |
| Host-visible time from trigger to readable content | `DeviceWidgetContentTest` |
| Widgets drawn by a real launcher, Android-version differences | the API matrix in `instrumented.yml` |

## Measuring, not guessing

Two numbers exist and they measure different things. Neither may be quoted as the other.

| Number | What it is | Where |
|---|---|---|
| Composition time | `provideGlance` plus `RemoteViews` translation, inside the test process, with the data fixed | `GlanceWidgetBenchmark` (`benchmark/micro`) |
| Host-visible time to content | From the update broadcast until a real `AppWidgetHost` shows readable content, host inflation included | `RenderedWidget.millisToContent` |

The first cannot see the launcher and says so in its own KDoc. The second is what the user waits for, but it is
polled at 200 ms, so it is an upper bound rounded up to that interval, not a precise figure.

Each case in `GlanceWidgetBenchmark` composes at `WidgetKind.defaultSize` — the bucket the kind lays out for at its
own span — rather than at a size written beside the test. T-1216 was measured at `SMALL`, which it does not
support, for as long as the size was a parameter.

## What "rendered" means

`RenderedWidget` walks the view tree the host actually applied and records the non-blank texts, the images carrying a
drawable, and the progress bars. `assertRenderedFinalContent` fails when nothing was delivered, when the launcher's
error view appeared, or when what was delivered carries neither text nor image — which is the placeholder.

Insufficient, and not accepted as proof that a widget works:

- no exception was thrown
- `update()` returned
- the host received non-null `RemoteViews`
- a callback fired
- a coroutine completed

## Regression harvesting

Every widget bug found on a device gets a row here: what escaped, why the local layer could not see it, and what now
catches it. The point is that the next bug of that *class* is caught without a phone.

| Bug | Why Layer A missed it | Now caught by |
|---|---|---|
| Widgets stuck on the loading placeholder after upgrading to v1.3.0. `JobScheduler.cancelAll()` on `MY_PACKAGE_REPLACED` cancelled the Glance `AsyncRequestWorker` jobs that draw widgets. | Nothing expressed "the app must not cancel work it did not schedule". The premise that justified the cancel — "Taqvim schedules no job of any kind" — was checked by grepping *our* sources for `androidx.work`, which cannot see a library scheduling work on the app's behalf. | `UpgradeKeepsScheduledWorkTest` (local, fails on the previous commit) and a Konsist ban on `cancelAll()`, with a planted violation. Device mirror: `anUpgradeDoesNotCancelScheduledWorkOnADevice`. |
| The same bug survived a green device suite on four API levels. | The device assertion counted `RemoteViews` deliveries, and a widget that never composes delivers none — but every test placed its widget on a *fresh install*, the one situation in which neither broadcast fires. | `RenderedWidget` refuses a placeholder, and `DeviceWidgetContentTest` exercises the upgrade broadcast. |
| A silent dependency would hang `provideGlance` forever, and no test could express it. | The local layer only ever asked what the loader *returns*, never whether it returns at all; `runCatching` around a suspend that never resumes catches nothing. | `WidgetLoadNeverHangsTest`, against a bounded `WidgetStateLoader.load`. |
| T-1216 shipped as a 3x3 whose largest *fitting* bucket was `MEDIUM`, so a 180 dp widget drew type written for a 110 dp one. | Every size test asked what `forAvailable` returns for an arbitrary span. None asked whether a kind's own default span lands on a bucket of its own size, so the drift between the manifest and the layout had nothing to contradict it. | `widgetsAreLaidOutForTheSpanTheyAskFor`, which fails with "`THREE_DATES` spans 180.0x180.0 dp but lays out as MEDIUM". |
| The one performance figure for T-1216 described a `SMALL` composition the widget cannot be given. | The size was a parameter of the benchmark, so nothing related it to what the kind supports. | `WidgetKind.defaultSize`, which the benchmark now reads instead — the drift is unrepresentable rather than merely tested. |
| `WidgetDateLines` — the header twelve widgets draw — did not isolate its dates, although T-1216 isolated its three. A Gregorian secondary date on a Persian widget was laid out in the widget's direction. | The Glance tests assert on a semantics tree, which holds the string but not its visual order, so a reordered line reads identically to a correct one. | `theSharedDateHeaderIsolatesBothItsDates` for the controls, and `DateHeaderRenderingTest` for the picture — inflated `RemoteViews`, captured, committed. |
| DATE_1X1 laid its weekday and secondary date out past its own bottom edge, and DATE_CLOCK_4X1 measured its whole date column zero pixels wide beside the clock. Both ship: `minHeight` is 40 dp, so a launcher may give exactly that. | Nothing inflated a widget and looked at where its views landed. The device suite reads the host's tree for *some* readable text, and a widget whose lower lines fall off the bottom still has a first line — the 4x1 still had its clock. | `EveryWidgetFitsItsBoxTest`: every kind, at every span it offers, with real values, failing on any label outside the box or measuring zero. |
| The day line was cut to "10 Octob…" in a two-cell box. | `maxLines = 1` makes truncation the designed behaviour, so no assertion was violated; only a measurement of the ellipsis count shows it. | `DateHeaderRenderingTest` requires `getEllipsisCount == 0` for dates — a value the widget chose and knows the length of, unlike an event title, which may be any length and may ellipsize. |

## Known limits

- **The cancellation race is not reproducible on an emulator.** Restoring the bug leaves
  `aWidgetStillRendersAfterAnUpgradeOrRebootBroadcast` green: Glance finishes inside the broadcast's own window, so
  no job is pending to cancel. The sentinel-job test is what catches it, and it does so without depending on the
  race. Verified by restoring the bug and watching the sentinel test fail.
- **Instrumenting the minified build is possible but not faithful.** `-Ptaqvim.testBuildType=benchmark` runs the
  device suite against the R8 output users install. Making the test APK attach to it needs keep rules for the
  Kotlin stdlib and the test runner, which changes the very shrinking being tested, so it is a smoke check and not
  evidence about R8's effect on the app.
- **A real launcher is not a test host.** `AppWidgetHost` in a test process exercises the same binding and
  `RemoteViews` path, but not a launcher's own update scheduling, restore-after-reboot or resize behaviour. Those
  stay in `MANUAL_TEST_CHECKLIST.md` §1.
