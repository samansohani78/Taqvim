/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/** T-1205: each placed month widget keeps its own month across process death until it is removed. */
@RunWith(AndroidJUnit4::class)
class WidgetViewStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun viewsAreKeptPerWidgetAndForgottenOnRemoval(): Unit =
        runBlocking {
            val store = SharedPreferencesWidgetViewStore(context)
            store.view(1) shouldBe WidgetView()

            store.save(1, WidgetView(monthOffset = 3))
            store.save(2, WidgetView(monthOffset = -2))
            SharedPreferencesWidgetViewStore(context).view(1) shouldBe WidgetView(3)
            store.view(2) shouldBe WidgetView(-2)

            store.save(2, WidgetView())
            context.getSharedPreferences("taqvim_widget_views", Context.MODE_PRIVATE).all.keys shouldBe
                setOf("month_offset_1")
            store.delete(setOf(1, 9))
            store.view(1) shouldBe WidgetView()

            WidgetViewStore.NONE.save(1, WidgetView(5))
            WidgetViewStore.NONE.delete(setOf(1))
            WidgetViewStore.NONE.view(1) shouldBe WidgetView()
        }
}
