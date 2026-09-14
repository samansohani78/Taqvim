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
import ir.taqvim.core.model.Jdn
import ir.taqvim.feature.agenda.AgendaNavigation
import ir.taqvim.feature.calendar.CalendarMessage
import ir.taqvim.feature.calendar.CalendarNavigation
import ir.taqvim.feature.search.SearchNavigation
import ir.taqvim.feature.search.SettingsEntry
import ir.taqvim.feature.search.ToolEntry
import ir.taqvim.feature.settings.SettingsDestination
import ir.taqvim.feature.settings.SettingsItemId
import ir.taqvim.feature.settings.SettingsNavigation
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
 * actions and calendar messages for [showMessage]. Days open the calendar on that day, new events open the editor on
 * their day and times, and planetary hours open in the Astronomy screen (T-1103).
 */
internal class AppRouter(
    private val navigate: (AppDestination) -> Unit,
    private val external: ExternalActions,
    private val showMessage: (CalendarMessage) -> Unit,
) {
    fun calendar(): CalendarNavigation =
        CalendarNavigation(
            onOpenEventEditor = { navigate(AppDestination.EventEditor(day = it.value)) },
            onOpenEvent = { openEvent(EventOrigin.valueOf(it.kind.name), it.id) },
            onOpenTimeline = { navigate(AppDestination.Timeline(it.value)) },
            onMessage = showMessage,
            onOpenUrl = external::openUrl,
            onOpenSearch = { navigate(AppDestination.Search) },
            onOpenShiftWork = { navigate(AppDestination.Pending(PendingFeature.SHIFT_WORK)) },
            onOpenPlanetaryHours = { navigate(AppDestination.PlanetaryHours(it.value)) },
        )

    fun year(): YearNavigation = YearNavigation(onOpenMonth = { navigate(AppDestination.Day(it.value)) })

    fun agenda(): AgendaNavigation =
        AgendaNavigation(
            onOpenDay = { navigate(AppDestination.Day(it.value)) },
            onOpenEvent = { openEvent(EventOrigin.valueOf(it.kind.name), it.id) },
        )

    fun timeline(): TimelineNavigation =
        TimelineNavigation(
            onCreateEvent = { day, start, end ->
                navigate(AppDestination.EventEditor(day = day.value, startMinute = start, endMinute = end))
            },
            onOpenEvent = { id, kind -> openEvent(EventOrigin.valueOf(kind.name), id) },
        )

    fun search(): SearchNavigation =
        SearchNavigation(
            onOpenDay = { navigate(AppDestination.Day(it.value)) },
            onOpenEvent = { kind, id, day -> openEvent(EventOrigin.valueOf(kind.name), id, day) },
            onOpenSettings = { navigate(settingsDestination(it)) },
            onOpenTool = { navigate(toolDestination(it)) },
        )

    fun settings(): SettingsNavigation = SettingsNavigation(onOpen = { navigate(settingsPage(it)) })

    /**
     * Personal events open in the editor and device events in the calendar app; others show on the calendar, on their
     * [day] when it is known.
     */
    private fun openEvent(
        origin: EventOrigin,
        id: String,
        day: Jdn? = null,
    ) {
        val number = id.toLongOrNull()
        when {
            origin == EventOrigin.PERSONAL && number != null -> navigate(AppDestination.EventEditor(number))
            origin == EventOrigin.DEVICE && number != null -> external.openDeviceEvent(number)
            else -> navigate(day?.let { AppDestination.Day(it.value) } ?: AppDestination.Calendar)
        }
    }
}

/** The screen of a settings search result: backup, privacy or the settings home at its item; about has no screen yet. */
internal fun settingsDestination(entry: SettingsEntry): AppDestination =
    when (entry) {
        SettingsEntry.BACKUP -> {
            AppDestination.Backup
        }

        SettingsEntry.PRIVACY -> {
            AppDestination.Privacy
        }

        else -> {
            SETTINGS_ITEMS[entry]?.let { AppDestination.Settings(it.name) }
                ?: AppDestination.Pending(PendingFeature.SETTINGS)
        }
    }

/** The settings item each settings search result opens. */
private val SETTINGS_ITEMS: Map<SettingsEntry, SettingsItemId> =
    mapOf(
        SettingsEntry.LANGUAGE to SettingsItemId.LANGUAGE,
        SettingsEntry.LOCATION to SettingsItemId.LOCATION,
        SettingsEntry.CALENDARS to SettingsItemId.MAIN_CALENDAR,
        SettingsEntry.PRAYER_TIMES to SettingsItemId.PRAYER_METHOD,
        SettingsEntry.ATHAN to SettingsItemId.ATHAN,
        SettingsEntry.NOTIFICATIONS to SettingsItemId.PERSISTENT_NOTIFICATION,
        SettingsEntry.THEME to SettingsItemId.THEME,
        SettingsEntry.WIDGETS to SettingsItemId.WIDGETS,
    )

/** The screen a settings row opens. */
internal fun settingsPage(destination: SettingsDestination): AppDestination =
    when (destination) {
        SettingsDestination.LOCATION -> AppDestination.LocationSettings
        SettingsDestination.ATHAN -> AppDestination.AthanSettings
        SettingsDestination.SUBSCRIPTIONS -> AppDestination.Subscriptions
        SettingsDestination.WIDGETS -> AppDestination.Pending(PendingFeature.WIDGETS)
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
