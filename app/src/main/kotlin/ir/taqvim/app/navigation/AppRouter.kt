/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.navigation

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import ir.taqvim.feature.agenda.AgendaNavigation
import ir.taqvim.feature.calendar.CalendarMessage
import ir.taqvim.feature.calendar.CalendarNavigation
import ir.taqvim.feature.search.SearchNavigation
import ir.taqvim.feature.search.SettingsEntry
import ir.taqvim.feature.search.ToolEntry
import ir.taqvim.feature.timeline.TimelineNavigation
import ir.taqvim.feature.year.YearNavigation

/** Where an event on any screen comes from; every feature's event kind has these names. */
internal enum class EventOrigin {
    OFFICIAL,
    PERSONAL,
    DEVICE,
    SUBSCRIPTION,
}

/** What the app does outside its own screens. */
internal interface ExternalActions {
    /** Opens a web page, e.g. a cited primary source. */
    fun openUrl(url: String)

    /** Opens the device calendar's event [id]. */
    fun openDeviceEvent(id: Long)
}

/** [ExternalActions] through [context]: web pages in a Custom Tab (or any browser), events in the calendar app. */
internal class ContextExternalActions(
    private val context: Context,
) : ExternalActions {
    override fun openUrl(url: String) {
        val uri = url.toUri()
        if (uri.scheme !in WEB_SCHEMES) return
        val customTab = runCatching { CustomTabsIntent.Builder().build().launchUrl(context, uri) }
        if (customTab.isFailure) start(Intent(Intent.ACTION_VIEW, uri))
    }

    override fun openDeviceEvent(id: Long) {
        start(Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id)))
    }

    private fun start(intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    private companion object {
        val WEB_SCHEMES = setOf("http", "https")
    }
}

/**
 * Where feature screens lead (ADR-0015): each feature's navigation callbacks as [navigate] destinations, [external]
 * actions and calendar messages for [showMessage]. Features without an entry point for a day or month (calendar, year,
 * agenda, search) open the calendar screen itself.
 */
internal class AppRouter(
    private val navigate: (AppDestination) -> Unit,
    private val external: ExternalActions,
    private val showMessage: (CalendarMessage) -> Unit,
) {
    fun calendar(): CalendarNavigation =
        CalendarNavigation(
            onOpenEventEditor = { navigate(AppDestination.EventEditor()) },
            onOpenEvent = { openEvent(EventOrigin.valueOf(it.kind.name), it.id) },
            onOpenTimeline = { navigate(AppDestination.Timeline(it.value)) },
            onMessage = showMessage,
            onOpenUrl = external::openUrl,
            onOpenSearch = { navigate(AppDestination.Search) },
            onOpenShiftWork = { navigate(AppDestination.Pending(PendingFeature.SHIFT_WORK)) },
            onOpenPlanetaryHours = { navigate(AppDestination.Astronomy) },
        )

    fun year(): YearNavigation = YearNavigation(onOpenMonth = { navigate(AppDestination.Calendar) })

    fun agenda(): AgendaNavigation =
        AgendaNavigation(
            onOpenDay = { navigate(AppDestination.Calendar) },
            onOpenEvent = { openEvent(EventOrigin.valueOf(it.kind.name), it.id) },
        )

    fun timeline(): TimelineNavigation =
        TimelineNavigation(
            onCreateEvent = { _, _, _ -> navigate(AppDestination.EventEditor()) },
            onOpenEvent = { id, kind -> openEvent(EventOrigin.valueOf(kind.name), id) },
        )

    fun search(): SearchNavigation =
        SearchNavigation(
            onOpenDay = { navigate(AppDestination.Calendar) },
            onOpenEvent = { kind, id, _ -> openEvent(EventOrigin.valueOf(kind.name), id) },
            onOpenSettings = { navigate(settingsDestination(it)) },
            onOpenTool = { navigate(toolDestination(it)) },
        )

    /** Personal events open in the editor and device events in the calendar app; others show on the calendar. */
    private fun openEvent(
        origin: EventOrigin,
        id: String,
    ) {
        val number = id.toLongOrNull()
        when {
            origin == EventOrigin.PERSONAL && number != null -> navigate(AppDestination.EventEditor(number))
            origin == EventOrigin.DEVICE && number != null -> external.openDeviceEvent(number)
            else -> navigate(AppDestination.Calendar)
        }
    }
}

/** The screen of a settings search result; settings without their own screen yet show a notice (T-1500). */
internal fun settingsDestination(entry: SettingsEntry): AppDestination =
    when (entry) {
        SettingsEntry.LOCATION -> AppDestination.LocationSettings
        SettingsEntry.ATHAN, SettingsEntry.PRAYER_TIMES -> AppDestination.AthanSettings
        else -> AppDestination.Pending(PendingFeature.SETTINGS)
    }

/** The screen of a tool search result. */
internal fun toolDestination(entry: ToolEntry): AppDestination =
    when (entry) {
        ToolEntry.COMPASS -> AppDestination.Compass
        ToolEntry.LEVEL -> AppDestination.Level
        ToolEntry.ASTRONOMY -> AppDestination.Astronomy
        ToolEntry.PRAYER_TIMES -> AppDestination.Times
        ToolEntry.AGENDA -> AppDestination.Agenda
        ToolEntry.YEAR -> AppDestination.Year
        else -> AppDestination.Tools
    }
