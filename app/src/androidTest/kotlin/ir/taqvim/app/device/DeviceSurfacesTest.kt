/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.device

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutManager
import android.view.View
import android.widget.RemoteViews
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import ir.taqvim.app.MainActivity
import ir.taqvim.feature.notification.PersistentNotifications
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Taqvim's system surfaces on a real Android device (T-1200–T-1215): every widget is bound, updated and drawn without
 * the launcher's error view, the persistent notification is posted, and the tile and shortcuts resolve.
 */
class DeviceSurfacesTest {
    @get:Rule
    val notifications: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val host = RecordingHost(appContext)

    @Before
    fun setUp() {
        useLanguage("fa")
        shell("appwidget grantbind --package ${appContext.packageName} --user 0")
    }

    @After
    fun tearDown() {
        onMain { host.stopListening() }
        host.deleteHost()
    }

    @Test
    fun everyWidgetIsBoundUpdatedAndDrawn() {
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

    @Test
    fun persistentNotificationIsPosted() {
        updatePreferences { it.copy(app = it.app.copy(persistentNotification = true)) }
        val manager = appContext.getSystemService(NotificationManager::class.java)
        waitFor("the persistent notification") {
            manager.activeNotifications.any { it.id == PersistentNotifications.ID }
        }
        updatePreferences { it.copy(app = it.app.copy(persistentNotification = false)) }
        waitFor("the persistent notification to go away") {
            manager.activeNotifications.none { it.id == PersistentNotifications.ID }
        }
    }

    @Test
    fun tileAndShortcutsResolve() {
        val packageManager = appContext.packageManager
        val tile = Intent("android.service.quicksettings.action.QS_TILE").setPackage(appContext.packageName)
        check(packageManager.queryIntentServices(tile, PackageManager.GET_META_DATA).isNotEmpty()) {
            "No Quick Settings tile service"
        }
        val shortcuts = appContext.getSystemService(ShortcutManager::class.java)
        (shortcuts.manifestShortcuts + shortcuts.dynamicShortcuts).forEach { shortcut ->
            val intent = checkNotNull(shortcut.intent) { "Shortcut ${shortcut.id} has no intent" }
            check(packageManager.resolveActivity(intent, 0) != null) { "Shortcut ${shortcut.id} does not resolve" }
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
        const val HOST_ID = 0x7a51
        const val WIDGET_COUNT = 12
    }
}
