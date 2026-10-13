/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.security

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.app.MainActivity
import ir.taqvim.app.finishOnboarding
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf

/**
 * ADR-0055 and T-1001/T-1101/T-1213: starting the app asks for nothing.
 *
 * Taqvim supports notifications, exact alarms and Do Not Disturb access, and a user who opens it for the first time
 * should be shown a calendar, not a stack of system dialogs. The permission is asked for where the user turns the
 * feature on; the tests of those screens prove it is asked for there, and this one proves it is asked nowhere else
 * on the way in.
 */
@RunWith(AndroidJUnit4::class)
class StartupAsksNothingTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun openingTheAppRequestsNoPermission() {
        composeRule.finishOnboarding()
        composeRule.waitForIdle()

        val activity = shadowOf(composeRule.activity)
        activity.lastRequestedPermission.shouldBeNull()
        // Not even an intent to a system permission screen, which would be the same nagging by another route.
        shadowOf(composeRule.activity.application).nextStartedActivity.shouldBeNull()
    }

    @Test
    fun theFirstRunOnboardingRequestsNoPermissionEither() {
        // The onboarding is the one screen every user sees, and the tempting place to ask for everything at once.
        composeRule.waitForIdle()

        shadowOf(composeRule.activity).lastRequestedPermission.shouldBeNull()
        composeRule.activity.isFinishing shouldBe false
    }
}
