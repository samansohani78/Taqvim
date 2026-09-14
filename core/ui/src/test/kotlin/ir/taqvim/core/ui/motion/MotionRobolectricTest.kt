/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.motion

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment

@RunWith(AndroidJUnit4::class)
class MotionRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readsTheSystemAnimatorScale() {
        val resolver = RuntimeEnvironment.getApplication().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        var motion: MotionSettings? = null

        composeRule.setContent { motion = rememberSystemMotionSettings() }

        composeRule.runOnIdle {
            assertEquals(MotionSettings(0f), motion)
            assertEquals(true, motion?.reduced)
        }
    }

    @Test
    fun sharedBoundsApplyInsideASharedTransitionAndAreANoOpOutside() {
        var outside: Modifier? = null

        var withoutKey: Modifier? = null
        var inside: Modifier? = null

        composeRule.setContent {
            Column {
                WithSharedBounds(SharedKey.Day(1)) { outside = it }
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        ProvideSharedScopes(this@SharedTransitionLayout, this) {
                            WithSharedBounds(null) { withoutKey = it }
                            WithSharedBounds(SharedKey.Event("1")) { shared ->
                                inside = shared
                                Box(Modifier.size(10.dp).then(shared).testTag("shared"))
                            }
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithTag("shared").assertExists()
        composeRule.runOnIdle {
            assertSame(Modifier, outside)
            assertSame(Modifier, withoutKey)
            assertNotSame(Modifier, inside)
        }
    }
}
