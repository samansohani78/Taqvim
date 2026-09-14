/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Where a widget's own navigation stands: the month the interactive month widget shows, relative to today's. */
data class WidgetView(
    val monthOffset: Int = 0,
)

/** Per-widget [WidgetView]s keyed by app widget id; forgotten when the widgets are removed. */
interface WidgetViewStore {
    suspend fun view(appWidgetId: Int): WidgetView

    suspend fun save(
        appWidgetId: Int,
        view: WidgetView,
    )

    suspend fun delete(appWidgetIds: Set<Int>)

    companion object {
        /** No navigation: every widget shows today's month. */
        val NONE: WidgetViewStore = NoWidgetViews
    }
}

private object NoWidgetViews : WidgetViewStore {
    override suspend fun view(appWidgetId: Int): WidgetView = WidgetView()

    override suspend fun save(
        appWidgetId: Int,
        view: WidgetView,
    ) = Unit

    override suspend fun delete(appWidgetIds: Set<Int>) = Unit
}

/**
 * [WidgetViewStore] in a private preferences file: the shown month survives process death, and today's month (offset
 * 0) is stored as the absence of a value.
 */
class SharedPreferencesWidgetViewStore(
    context: Context,
) : WidgetViewStore {
    private val preferences: SharedPreferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val mutex = Mutex()

    override suspend fun view(appWidgetId: Int): WidgetView =
        mutex.withLock { WidgetView(preferences.getInt(key(appWidgetId), 0)) }

    override suspend fun save(
        appWidgetId: Int,
        view: WidgetView,
    ): Unit =
        mutex.withLock {
            preferences.edit(commit = true) {
                if (view.monthOffset == 0) remove(key(appWidgetId)) else putInt(key(appWidgetId), view.monthOffset)
            }
        }

    override suspend fun delete(appWidgetIds: Set<Int>): Unit =
        mutex.withLock { preferences.edit(commit = true) { appWidgetIds.forEach { remove(key(it)) } } }

    private fun key(appWidgetId: Int): String = "$MONTH_OFFSET_PREFIX$appWidgetId"

    private companion object {
        const val FILE = "taqvim_widget_views"
        const val MONTH_OFFSET_PREFIX = "month_offset_"
    }
}

/** The month steps of the interactive month widget (T-1205): a delta of months, or back to today's month. */
object WidgetMonthStep {
    /** Months to move; `0` returns to today's month. */
    val DELTA: ActionParameters.Key<Int> = ActionParameters.Key("ir.taqvim.feature.widgets.month_delta")

    /** The offset after moving [delta] months from [current]; `0` or a missing delta returns to today's month. */
    fun next(
        current: Int,
        delta: Int?,
    ): Int = if (delta == null || delta == 0) 0 else WidgetCalendarBuilder.clampOffset(current + delta)

    fun parameters(delta: Int): ActionParameters = actionParametersOf(DELTA to delta)
}

/** Moves a placed interactive month widget by the [WidgetMonthStep.DELTA] of its tapped control and redraws it. */
class MonthStepCallback :
    ActionCallback,
    KoinComponent {
    private val views: WidgetViewStore by inject()

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val view = views.view(appWidgetId)
        val offset = WidgetMonthStep.next(view.monthOffset, parameters[WidgetMonthStep.DELTA])
        views.save(appWidgetId, view.copy(monthOffset = offset))
        MonthInteractiveWidget().update(context, glanceId)
    }
}
