/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/** T-1201…T-1204: every widget is registered once, declared as an exported launcher receiver and names its prayers. */
@RunWith(AndroidJUnit4::class)
class WidgetCatalogTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun eachWidgetIsRegisteredOnceWithItsOwnReceiverAndWidget() {
        val registrations = WidgetCatalog.registrations
        registrations.map { it.kind } shouldContainExactlyInAnyOrder
            listOf(
                WidgetKind.DATE_1X1,
                WidgetKind.DATE_CLOCK_4X1,
                WidgetKind.DAY_SUMMARY_2X2,
                WidgetKind.PRAYER_STRIP_4X2,
            )
        registrations.forEach { registration ->
            val created = registration.create()
            created::class.java shouldBe registration.widget
            created.shouldBeInstanceOf<TaqvimGlanceWidget>().kind shouldBe registration.kind
            registration.receiver
                .getDeclaredConstructor()
                .newInstance()
                .glanceAppWidget::class.java shouldBe
                registration.widget
        }
    }

    @Test
    fun receiversAreExportedLauncherWidgetsWithProviderMetadata() {
        WidgetCatalog.registrations.forEach { registration ->
            val info =
                context.packageManager.getReceiverInfo(
                    ComponentName(context, registration.receiver),
                    PackageManager.GET_META_DATA,
                )
            info.exported shouldBe true
            info.metaData.getInt("android.appwidget.provider") shouldNotBe 0
        }
    }

    @Test
    fun theKoinModuleRegistersTheCatalogAndPrayerNames() {
        val koin = koinApplication { modules(widgetsFeatureModule, contextModule()) }.koin
        koin.getAll<WidgetRegistration>() shouldContainExactlyInAnyOrder WidgetCatalog.registrations
        val names = koin.get<WidgetPrayerNames>()
        names.name(WidgetPrayer.MAGHRIB) shouldBe context.getString(R.string.widget_prayer_maghrib)
        WidgetPrayer.entries
            .map { names.name(it) }
            .toSet()
            .size shouldBe WidgetPrayer.entries.size
    }

    private fun contextModule(): Module = module { single<Context> { context } }
}
