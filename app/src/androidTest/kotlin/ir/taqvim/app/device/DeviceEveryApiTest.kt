/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.device

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import android.widget.RemoteViews
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import ir.taqvim.app.MainActivity
import org.junit.After
import org.junit.Test

/**
 * The two claims of ADR-0054 and ADR-0055 that have to hold on **every** API level the app supports, checked on the
 * device rather than argued from the manifest: the widgets draw without `ACCESS_NETWORK_STATE`, and the app runs
 * holding no runtime permission at all.
 *
 * Every other class in this package is `@SdkSuppress(minSdkVersion = TIRAMISU)`, because they set the per-app
 * language through `LocaleManager` (API 33, ADR-0023) or need `POST_NOTIFICATIONS` granted. That is the right
 * trade for them, but it left the API 26 and API 30 legs of `instrumented.yml` running nothing but
 * [DeviceJourneyCoverageTest], which touches no Android surface — so the two levels that are most likely to
 * disagree with a dependency's manifest merge verified none of it. This class takes no language and asks for no
 * permission, so it runs on all four.
 */
class DeviceEveryApiTest {
    private val host = RecordingHost(appContext)

    @After
    fun tearDown() {
        onMain { host.stopListening() }
        host.deleteHost()
    }

    /**
     * ADR-0054: `ACCESS_NETWORK_STATE` is removed at the merge point, although Glance still pulls in WorkManager,
     * which declares it. Glance's session work carries no network constraint, so WorkManager never starts the
     * connectivity tracker that would need it — and a widget that still binds, updates and draws on API 26 through
     * API 36 is the evidence for that, rather than a reading of a library's source.
     */
    @Test
    fun everyWidgetIsBoundUpdatedAndDrawnWithoutNetworkState() {
        keepScreenOn()
        updatePreferences { it.copy(onboardingCompleted = true) }
        shell("appwidget grantbind --package ${appContext.packageName} --user 0")
        val manager = AppWidgetManager.getInstance(appContext)
        val providers = manager.getInstalledProvidersForPackage(appContext.packageName, null)
        check(providers.size >= WIDGET_COUNT) { "Only ${providers.size} widget providers are installed" }
        onMain { host.startListening() }
        // Widget views are drawn in an activity's context, as a launcher draws them.
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var activity: Activity? = null
            scenario.onActivity { activity = it }
            val context = checkNotNull(activity)
            providers.forEach { info -> bindAndDraw(manager, info, context) }
        }
    }

    /**
     * ADR-0055 and the permission timing of T-1001/T-1101/T-1213: the app opens, draws its calendar and keeps
     * running while holding none of the permissions it was once going to need, and asks for nothing on the way in.
     */
    @Test
    fun theAppOpensHoldingNoRuntimePermission() {
        keepScreenOn()
        updatePreferences { it.copy(onboardingCompleted = true) }
        openLink("taqvim://calendar", "destination:Calendar")
        assertInFront()

        WITHHELD.forEach { permission ->
            check(appContext.checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
                "$permission is granted; the app is not supposed to hold it"
            }
        }
        // A permission dialog in front would mean the app asked for something at startup.
        check(!device.currentPackageName.orEmpty().contains("permissioncontroller")) {
            "a permission dialog is in front after opening the app"
        }
    }

    private fun bindAndDraw(
        manager: AppWidgetManager,
        info: AppWidgetProviderInfo,
        context: Context,
    ) {
        val id = host.allocateAppWidgetId()
        check(manager.bindAppWidgetIdIfAllowed(id, info.provider)) { "Cannot bind ${info.provider}" }
        val view = checkNotNull(onMain { host.createView(context, id, info) as? RecordingHostView })
        val update =
            Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .setComponent(info.provider)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(id))
        appContext.sendBroadcast(update)
        waitFor("${info.provider.shortClassName} to draw") { view.updates > 0 }
        check(!view.failed) { "${info.provider.shortClassName} showed the error view" }
        host.deleteAppWidgetId(id)
    }

    private fun <T> onMain(block: () -> T): T {
        var result: Result<T>? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result = runCatching(block) }
        return checkNotNull(result).getOrThrow()
    }

    /** A widget host whose views record their updates and whether the error view was shown. */
    private class RecordingHost(
        context: Context,
    ) : AppWidgetHost(context, HOST_ID) {
        override fun onCreateView(
            context: Context,
            appWidgetId: Int,
            appWidget: AppWidgetProviderInfo?,
        ): AppWidgetHostView = RecordingHostView(context)
    }

    private class RecordingHostView(
        context: Context,
    ) : AppWidgetHostView(context) {
        @Volatile
        var updates: Int = 0

        @Volatile
        var failed: Boolean = false

        override fun updateAppWidget(remoteViews: RemoteViews?) {
            super.updateAppWidget(remoteViews)
            if (remoteViews != null) updates++
        }

        override fun getErrorView(): View {
            failed = true
            return super.getErrorView()
        }
    }

    private companion object {
        /** Not [DeviceSurfacesTest]'s host id: both classes may run in the same process. */
        const val HOST_ID = 0x7a52
        const val WIDGET_COUNT = 12

        /** Permissions the app must work without, because it never asks for any of them. */
        val WITHHELD =
            listOf(
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR,
            )
    }
}
