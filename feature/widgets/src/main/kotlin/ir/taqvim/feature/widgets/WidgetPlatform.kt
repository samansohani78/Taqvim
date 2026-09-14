/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * One widget of T-1201…T-1212 as registered with the framework (a Koin `single` per widget, collected with `getAll`):
 * its [kind], its Glance [receiver] and [widget] classes and a [create] factory.
 */
data class WidgetRegistration(
    val kind: WidgetKind,
    val receiver: Class<out GlanceAppWidgetReceiver>,
    val widget: Class<out GlanceAppWidget>,
    val create: () -> GlanceAppWidget,
)

/** [InstalledWidgets], [WidgetUpdater] and [WidgetKindResolver] over Glance and the platform `AppWidgetManager`. */
class GlanceWidgets(
    private val context: Context,
    private val registrations: List<WidgetRegistration>,
) : InstalledWidgets,
    WidgetUpdater,
    WidgetKindResolver {
    override suspend fun installed(): InstalledWidgetIds {
        val manager = GlanceAppWidgetManager(context)
        return registrations
            .groupBy { it.kind }
            .mapValues { (_, sameKind) ->
                sameKind.flatMap { manager.getGlanceIds(it.widget) }.map { manager.getAppWidgetId(it) }.toSet()
            }.filterValues { it.isNotEmpty() }
    }

    override suspend fun update(targets: InstalledWidgetIds) {
        val manager = GlanceAppWidgetManager(context)
        registrations.distinctBy { it.kind }.filter { it.kind in targets }.forEach { registration ->
            val widget = registration.create()
            targets.getValue(registration.kind).forEach { id -> widget.update(context, manager.getGlanceIdBy(id)) }
        }
    }

    override fun kindOf(appWidgetId: Int): WidgetKind? {
        val provider = AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)?.provider ?: return null
        return registrations.firstOrNull { it.receiver.name == provider.className }?.kind
    }
}

/**
 * [WidgetWakeUpScheduler] on `AlarmManager`: one inexact wall-clock alarm (`RTC`, not waking the device; a sleeping
 * device refreshes its widgets when it wakes) that broadcasts [WidgetBroadcasts.ACTION_WAKE_UP] to
 * [WidgetUpdateReceiver]. Scheduling again replaces the alarm.
 */
class AlarmWidgetWakeUpScheduler(
    private val context: Context,
) : WidgetWakeUpScheduler {
    private val alarmManager: AlarmManager =
        requireNotNull(context.getSystemService(AlarmManager::class.java)) { "AlarmManager is unavailable" }

    override fun schedule(wakeUp: WidgetWakeUp) {
        alarmManager.set(AlarmManager.RTC, wakeUp.at.toEpochMilliseconds(), operation(wakeUp.triggers))
    }

    override fun cancel() {
        val operation = operation(emptySet())
        alarmManager.cancel(operation)
        operation.cancel()
    }

    private fun operation(triggers: Set<WidgetUpdateTrigger>): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, WidgetUpdateReceiver::class.java)
                .setAction(WidgetBroadcasts.ACTION_WAKE_UP)
                .putExtra(WidgetBroadcasts.EXTRA_TRIGGERS, WidgetBroadcasts.namesOf(triggers)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private companion object {
        const val REQUEST_CODE = 0x57_1D_6E
    }
}

/** Receives the framework's wake-ups, clock/time-zone/language/app-update broadcasts and widget removals. */
class WidgetUpdateReceiver :
    BroadcastReceiver(),
    KoinComponent {
    private val refresher: WidgetRefresher by inject()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == WidgetBroadcasts.ACTION_DELETED) {
            val ids = intent.getIntArrayExtra(WidgetBroadcasts.EXTRA_APP_WIDGET_IDS)?.toSet().orEmpty()
            handleAsync { refresher.onDeleted(ids) }
            return
        }
        val triggers =
            WidgetBroadcasts.triggersFor(intent.action, intent.getStringArrayExtra(WidgetBroadcasts.EXTRA_TRIGGERS))
                ?: return
        handleAsync { if (triggers.isEmpty()) refresher.reschedule() else refresher.refresh(triggers) }
    }

    companion object {
        /** Asks the framework to re-plan its wake-up (a widget was placed). */
        fun rescheduleIntent(context: Context): Intent =
            Intent(context, WidgetUpdateReceiver::class.java).setAction(WidgetBroadcasts.ACTION_RESCHEDULE)

        /** Tells the framework that [appWidgetIds] were removed. */
        fun deletedIntent(
            context: Context,
            appWidgetIds: IntArray,
        ): Intent =
            Intent(context, WidgetUpdateReceiver::class.java)
                .setAction(WidgetBroadcasts.ACTION_DELETED)
                .putExtra(WidgetBroadcasts.EXTRA_APP_WIDGET_IDS, appWidgetIds)
    }
}

/**
 * Base receiver of Taqvim's widgets: Glance draws them, and placing or removing one tells [WidgetUpdateReceiver] so the
 * wake-up plan and stored configurations follow the installed widgets. Widget receivers (T-1201…) are declared
 * `android:exported="true"` with the `APPWIDGET_UPDATE` filter because the launcher sends that broadcast, and each
 * one is listed in the exported-components allowlist (ADR-0017).
 */
abstract class TaqvimWidgetReceiver : GlanceAppWidgetReceiver() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.sendBroadcast(WidgetUpdateReceiver.rescheduleIntent(context))
    }

    override fun onDeleted(
        context: Context,
        appWidgetIds: IntArray,
    ) {
        super.onDeleted(context, appWidgetIds)
        context.sendBroadcast(WidgetUpdateReceiver.deletedIntent(context, appWidgetIds))
    }
}

/** Runs [block] off the main thread and keeps the broadcast alive until it completes; failures are rethrown. */
private fun BroadcastReceiver.handleAsync(block: suspend () -> Unit) {
    val pending: BroadcastReceiver.PendingResult? = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        val result = runCatching { block() }
        pending?.finish()
        result.getOrThrow()
    }
}
