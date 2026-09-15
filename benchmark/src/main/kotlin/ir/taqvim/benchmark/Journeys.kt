/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.benchmark

import android.content.Intent
import android.net.Uri
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

/** Application id of the app under test. */
internal const val APP_PACKAGE = "ir.taqvim.app"

private const val UI_TIMEOUT_MILLIS = 5_000L
private const val ONBOARDING_WAIT_MILLIS = 1_000L
private const val GESTURE_MARGIN_DIVISOR = 5

/** Test tags of the screens the journeys drive, exposed as resource ids by `MainActivity` (T-1801). */
internal object Tags {
    /** `MONTH_PAGER_TAG` of `:feature:calendar`. */
    const val MONTH_PAGER = "month_pager"

    /** `GRID_TAG` of `:feature:timeline`. */
    const val TIMELINE_GRID = "timeline_grid"

    /** `SEARCH_FIELD_TAG` of `:feature:search`. */
    const val SEARCH_FIELD = "search_field"

    /** `SEARCH_RESULTS_TAG` of `:feature:search`. */
    const val SEARCH_RESULTS = "search_results"

    /** `OnboardingTags.SKIP` of `:feature:settings`. */
    const val ONBOARDING_SKIP = "onboarding:skip"

    /** `tabTag(TopLevelTab.MORE)` of `:app`. */
    const val MORE_TAB = "tab:MORE"

    /** The year entry of the More screen (`MoreEntry.YEAR` in `:app`). */
    const val MORE_YEAR = "more:YEAR"

    /** `YEAR_PAGER_TAG` of `:feature:year`. */
    const val YEAR_PAGER = "year_pager"

    /** `YEAR_GRID_TAG` of `:feature:year`. */
    const val YEAR_GRID = "year_grid"

    /** `YEAR_NEXT_TAG` of `:feature:year`. */
    const val YEAR_NEXT = "year:next"

    /** `YEAR_PREVIOUS_TAG` of `:feature:year`. */
    const val YEAR_PREVIOUS = "year:previous"
}

/** Documented `taqvim://` links (ADR-0016) that open the screens of the journeys. */
internal object Links {
    const val NOWRUZ_1405 = "taqvim://day/1405-01-01"
    const val TIMELINE = "taqvim://timeline/1405-01-01"
    const val TIMES = "taqvim://times"
    const val SEARCH = "taqvim://search"
}

/** Starts Taqvim on [link], waits for its first frame and skips the first-run onboarding (T-1501) if it is shown. */
internal fun MacrobenchmarkScope.openLink(link: String) {
    startActivityAndWait(Intent(Intent.ACTION_VIEW, Uri.parse(link)).setPackage(APP_PACKAGE))
    skipOnboarding()
}

/** Skips the first-run onboarding (T-1501) shown on a fresh install, so the journeys reach their screens. */
internal fun MacrobenchmarkScope.skipOnboarding() {
    device.wait(Until.findObject(By.res(Tags.ONBOARDING_SKIP)), ONBOARDING_WAIT_MILLIS)?.click()
    device.waitForIdle()
}

/** The node tagged [tag], failing the run when the screen does not show it in time. */
internal fun MacrobenchmarkScope.waitForTag(tag: String): UiObject2 =
    checkNotNull(device.wait(Until.findObject(By.res(tag)), UI_TIMEOUT_MILLIS)) { "$tag is not shown" }

/** [node] with gesture margins that keep swipes away from the system gesture areas at the screen edges. */
internal fun MacrobenchmarkScope.withSafeGestureMargin(node: UiObject2): UiObject2 =
    node.apply { setGestureMargin(device.displayWidth / GESTURE_MARGIN_DIVISOR) }
