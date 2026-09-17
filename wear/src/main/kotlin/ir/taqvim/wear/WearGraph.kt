/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import android.app.Application
import android.content.Context
import ir.taqvim.data.devicecalendar.DeviceTimeZone
import ir.taqvim.data.events.generated.OfficialEvents
import ir.taqvim.data.location.CityCatalog
import ir.taqvim.data.preferences.UserPreferencesRepository
import java.util.Locale
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.TimeZone

/** Something that holds the watch's object graph: the application, or a test application. */
interface WearGraphOwner {
    val graph: WearGraph
}

/** The watch application; it owns one [WearGraph] for the process. */
open class TaqvimWearApplication :
    Application(),
    WearGraphOwner {
    override val graph: WearGraph by lazy { WearGraph.create(this) }
}

/** The graph of [context]'s application. */
internal fun Context.wearGraph(): WearGraph =
    requireNotNull(applicationContext as? WearGraphOwner) { "the application must provide a WearGraph" }.graph

/**
 * What the watch app, tiles and complications share: the preferences stored on the watch, the clock, the device time
 * zone, the bundled city catalog and the day calculator over the bundled official events (T-1600, ADR-0019).
 */
class WearGraph(
    val preferences: UserPreferencesRepository,
    val clock: Clock,
    private val deviceZones: Flow<TimeZone>,
    private val cityCatalog: Lazy<CityCatalog>,
    val calculator: WearDayCalculator,
) {
    /** The bundled city catalog, loaded on first use. */
    val catalog: CityCatalog
        get() = cityCatalog.value

    /** The watch setup, re-emitted after every preference change and every device time-zone change (review I06). */
    val setups: Flow<WearSetup> =
        combine(preferences.preferences, deviceZones.distinctUntilChanged()) { stored, zone ->
            stored.toWearSetup(zone) { id, code -> catalog.city(id)?.name(code) }
        }.flowOn(Dispatchers.Default)

    suspend fun setup(): WearSetup = setups.first()

    suspend fun today(): WearToday = calculator.today(setup(), clock.now())

    companion object {
        /** The production graph of [context]: one preferences store for the process and the system clock and zone. */
        fun create(
            context: Context,
            clock: Clock = Clock.System,
        ): WearGraph =
            WearGraph(
                preferences =
                    UserPreferencesRepository(
                        UserPreferencesRepository.createDataStore(
                            context = context,
                            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
                            deviceLanguage = { Locale.getDefault().language },
                        ),
                    ),
                clock = clock,
                deviceZones = DeviceTimeZone.changes(context),
                cityCatalog = lazy { CityCatalog.loadBundled() },
                calculator = WearDayCalculator(OfficialEvents.ALL),
            )
    }
}
