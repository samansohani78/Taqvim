/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import androidx.compose.ui.graphics.Color
import ir.taqvim.app.automation.AppShortcuts
import ir.taqvim.app.automation.LauncherIconRefresh
import ir.taqvim.app.automation.LauncherIconSwitcher
import ir.taqvim.core.astronomy.CelestialBody
import ir.taqvim.core.astronomy.Sky
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.ui.painter.MoonBitmapModel
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.notification.DailyRefresh
import ir.taqvim.feature.notification.DailyRefreshCoordinator
import ir.taqvim.feature.notification.PersistentNotificationOptions
import ir.taqvim.feature.notification.PersistentNotificationOptionsSource
import ir.taqvim.feature.notification.SummaryPrayer
import ir.taqvim.feature.notification.TodaySummary
import ir.taqvim.feature.notification.TodaySummarySource
import ir.taqvim.feature.wallpaper.DreamContent
import ir.taqvim.feature.wallpaper.DreamContentSource
import ir.taqvim.feature.wallpaper.WallpaperContent
import ir.taqvim.feature.wallpaper.WallpaperContentSource
import ir.taqvim.feature.widgets.WidgetConfig
import ir.taqvim.feature.widgets.WidgetDataSource
import ir.taqvim.feature.widgets.WidgetDrawings
import ir.taqvim.feature.widgets.WidgetKind
import ir.taqvim.feature.widgets.WidgetTimelineSource
import ir.taqvim.feature.widgets.WidgetView
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

private const val SEPARATOR = " · "

/**
 * [TodaySummarySource] (T-1213, T-1215) over the widgets' shared content (T-1200): the same day, calendars, language,
 * holidays and prayer times as the day summary widget, so every surface outside the app agrees.
 */
internal class WidgetTodaySummarySource(
    private val preferences: UserPreferencesRepository,
    private val data: WidgetDataSource,
    private val timeline: WidgetTimelineSource,
) : TodaySummarySource {
    override suspend fun load(now: Instant): TodaySummary {
        val prefs = preferences.preferences.first()
        val times = timeline.timeline(now)
        val day = data.load(WidgetKind.DAY_SUMMARY_2X2, WidgetConfig(), now, WidgetView())
        val primary = prefs.availableCalendars().firstOrNull() ?: PersianCalendarSystem
        return TodaySummary(
            dayOfMonth = primary.fromJdn(now.toJdn(times.timeZone)).day,
            dayNumber = day.dayNumber,
            title = day.title,
            weekday = day.weekday,
            otherDates = listOfNotNull(day.secondaryDate),
            holidays = day.events.filter { it.isHoliday }.map { it.title },
            prayers = day.prayers.map { SummaryPrayer(it.name, it.time, it.isNext) },
            nextDayAt = DailyRefreshCoordinator.startOfNextDay(now, times.timeZone),
            nextPrayerAt = times.nextPrayer,
        )
    }
}

/** [PersistentNotificationOptionsSource] over the T-1500 switches. */
internal class PreferencesPersistentNotificationOptions(
    private val preferences: UserPreferencesRepository,
) : PersistentNotificationOptionsSource {
    override suspend fun options(): PersistentNotificationOptions =
        preferences.preferences.first().app.let {
            PersistentNotificationOptions(it.persistentNotification, it.persistentNotificationLargeNumber)
        }
}

/** The wallpaper's sky at an instant: the Moon, day or night, and when that changes next. */
internal data class WallpaperSky(
    val moon: MoonBitmapModel,
    val daylight: Boolean,
    val nextChangeAt: Instant,
) {
    companion object {
        /** The Sun's centre at sunrise and sunset, allowing for refraction and its radius. */
        private const val HORIZON_DEGREES = -0.833
        private val EQUATOR = Coordinates(0.0, 0.0)
        private val DAY_START = LocalTime(6, 0)
        private val DAY_END = LocalTime(18, 0)

        /**
         * The sky at [now] for [observer] in [zone]. Without a chosen place day is 06:00–18:00 local time and the Moon is
         * drawn as seen from the equator (its phase is the same everywhere).
         */
        fun at(
            now: Instant,
            observer: Coordinates?,
            zone: TimeZone,
        ): WallpaperSky {
            val appearance = Sky.moonAppearance(now, observer ?: EQUATOR)
            val moon = MoonBitmapModel(appearance.illuminatedFraction.toFloat(), waxing = appearance.brightLimbOnRight)
            val nextDay = DailyRefreshCoordinator.startOfNextDay(now, zone)
            if (observer == null) {
                val local = now.toLocalDateTime(zone)
                val daylight = local.time >= DAY_START && local.time < DAY_END
                val boundaries = listOf(DAY_START, DAY_END).map { local.date.atTime(it).toInstant(zone) }
                return WallpaperSky(moon, daylight, (boundaries + nextDay).filter { it > now }.min())
            }
            val daylight = Sky.skyPosition(CelestialBody.SUN, now, observer).altitudeDegrees > HORIZON_DEGREES
            val sun = Sky.riseSetTransit(CelestialBody.SUN, observer, now)
            val next = listOfNotNull(sun.rise, sun.set, nextDay).filter { it > now }.min()
            return WallpaperSky(moon, daylight, next)
        }
    }
}

/** [WallpaperContentSource] (T-1215): the month widget's page (T-1206) under today's sky at the chosen place. */
internal class WidgetWallpaperContentSource(
    private val preferences: UserPreferencesRepository,
    private val data: WidgetDataSource,
    private val timeline: WidgetTimelineSource,
) : WallpaperContentSource {
    override suspend fun load(now: Instant): WallpaperContent {
        val place = preferences.preferences.first().widgetPlace()
        val times = timeline.timeline(now)
        val config = WidgetConfig()
        val day = data.load(WidgetKind.MONTH_BITMAP, config, now, WidgetView())
        val month = requireNotNull(day.month) { "The month widget content has no month page" }
        val sky = WallpaperSky.at(now, place?.coordinates, times.timeZone)
        return WallpaperContent(
            title = day.title,
            subtitle = listOfNotNull(day.weekday, day.secondaryDate).joinToString(SEPARATOR),
            month = WidgetDrawings.monthModel(month, config, EVENT_DOT),
            moon = sky.moon,
            daylight = sky.daylight,
            nextChangeAt = sky.nextChangeAt,
        )
    }

    private companion object {
        val EVENT_DOT = Color(0xFFFFD27A)
    }
}

/** [DreamContentSource] (T-1215): the weekday and date under the daydream's clock, with the secondary calendar. */
internal class WidgetDreamContentSource(
    private val data: WidgetDataSource,
    private val timeline: WidgetTimelineSource,
) : DreamContentSource {
    override suspend fun load(now: Instant): DreamContent {
        val times = timeline.timeline(now)
        val day = data.load(WidgetKind.DATE_1X1, WidgetConfig(), now, WidgetView())
        return DreamContent(
            date = "${day.weekday} ${day.title}".trim(),
            otherDates = day.secondaryDate.orEmpty(),
            nextChangeAt = DailyRefreshCoordinator.startOfNextDay(now, times.timeZone),
        )
    }
}

/** What the surfaces outside the app depend on in the preferences. */
internal fun UserPreferences.surfaceKey(): List<Any?> =
    listOf(
        app.persistentNotification,
        app.persistentNotificationLargeNumber,
        app.dynamicLauncherIcon,
        app.enabledEventSources,
        languageCode,
        calendars,
        numerals,
        islamicVariant,
        hijriOffsetDays,
        weekend,
        place,
        prayerSettings(),
    )

/**
 * Runs the [DailyRefreshCoordinator] (T-1213, T-1214) at start and whenever a preference the notification or the
 * launcher icon depends on changes, e.g. turning either on or off takes effect at once.
 */
internal class SurfaceRefreshWatcher(
    private val preferences: Flow<UserPreferences>,
    private val coordinator: DailyRefreshCoordinator,
) {
    suspend fun watch() {
        preferences.map { it.surfaceKey() }.distinctUntilChanged().collect { coordinator.run() }
    }
}

/** The persistent notification, launcher icon, tile, wallpaper and daydream (T-1213…T-1215) over the widget ports. */
val surfacePortsModule =
    module {
        single<TodaySummarySource> { WidgetTodaySummarySource(get(), get(), get()) }
        single<PersistentNotificationOptionsSource> { PreferencesPersistentNotificationOptions(get()) }
        single<WallpaperContentSource> { WidgetWallpaperContentSource(get(), get(), get()) }
        single<DreamContentSource> { WidgetDreamContentSource(get(), get()) }
        single { LauncherIconSwitcher(androidContext()) }
        single {
            val preferences = get<UserPreferencesRepository>()
            val context = androidContext()
            LauncherIconRefresh({
                preferences.preferences
                    .first()
                    .app.dynamicLauncherIcon
            }, get(), get()) { entry ->
                AppShortcuts.publish(context, entry)
            }
        } bind DailyRefresh::class
        single { SurfaceRefreshWatcher(get<UserPreferencesRepository>().preferences, get()) }
    }
