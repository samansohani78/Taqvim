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

### Layer B — real Android runtime (`androidTest/`, emulators and the owner's phone)

Slower, needs a device, and covers only what a JVM cannot fake:

| What | Where |
|---|---|
| Binding through a real `AppWidgetHost` and the `RemoteViews` the host applies | `DeviceWidgetContentTest` |
| Every one of the thirteen kinds reaching readable content | `DeviceWidgetContentTest` |
| Two instances of one kind rendering independently | `DeviceWidgetContentTest` |
| Scheduled work surviving an upgrade on a real JobScheduler | `DeviceWidgetContentTest` |
| Widgets drawn by a real launcher, Android-version differences | the API matrix in `instrumented.yml` |

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
