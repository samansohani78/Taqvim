/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear.device

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import ir.taqvim.wear.DateComplicationService
import ir.taqvim.wear.MonthProgressComplicationService
import ir.taqvim.wear.NextPrayerComplicationService
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T-1600 on a real watch: every complication provider installed on the device binds and declares the complication
 * types a watch face may ask for. The values each provider returns are checked on the JVM
 * (`WearTilesAndComplicationsTest`); a watch face cannot be driven from an instrumentation test, so §5 step 4 of
 * docs/MANUAL_TEST_CHECKLIST.md stays manual.
 */
@RunWith(AndroidJUnit4::class)
class WearComplicationDeviceTest {
    @get:Rule
    val service: ServiceTestRule = ServiceTestRule.withTimeout(BIND_TIMEOUT_SECONDS, TimeUnit.SECONDS)

    @Before
    fun setUp() {
        assumeWatch()
        useLanguage("fa")
    }

    @Test
    fun everyComplicationProviderBindsAndDeclaresItsTypes() {
        PROVIDERS.forEach { (provider, types) ->
            val component = ComponentName(appContext, provider)
            val intent = Intent(UPDATE_REQUEST_ACTION).setComponent(component)
            val binder = service.bindService(intent)
            check(binder != null) { "$provider did not bind" }

            val info =
                appContext.packageManager.getServiceInfo(
                    component,
                    PackageManager.GET_META_DATA or PackageManager.MATCH_ALL,
                )
            val declared = info.metaData?.getString(SUPPORTED_TYPES_METADATA)
            check(declared == types) { "$provider declares $declared, expected $types" }
            check(info.exported) { "$provider is not exported, so no watch face can use it" }
        }
    }

    private companion object {
        const val BIND_TIMEOUT_SECONDS = 30L

        /** What a watch face sends a complication provider (`ComplicationDataSourceService`). */
        const val UPDATE_REQUEST_ACTION =
            "android.support.wearable.complications.ACTION_COMPLICATION_UPDATE_REQUEST"

        const val SUPPORTED_TYPES_METADATA = "android.support.wearable.complications.SUPPORTED_TYPES"

        /** The three providers of the watch app and the type each declares in the manifest. */
        val PROVIDERS =
            listOf(
                DateComplicationService::class.java to "SHORT_TEXT",
                MonthProgressComplicationService::class.java to "RANGED_VALUE",
                NextPrayerComplicationService::class.java to "SHORT_TEXT",
            )
    }
}
