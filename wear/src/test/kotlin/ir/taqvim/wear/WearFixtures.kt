/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import android.app.Application
import ir.taqvim.core.model.Coordinates
import ir.taqvim.data.events.generated.OfficialEvents
import ir.taqvim.data.location.CityCatalog
import ir.taqvim.data.preferences.ChosenPlace
import ir.taqvim.data.preferences.PlaceSource
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.UserPreferencesRepository
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.datetime.TimeZone

/** Shared test data: rounded sample places, fixed instants and the calculator over the generated dataset. */
object WearFixtures {
    val TEHRAN_ZONE: TimeZone = TimeZone.of("Asia/Tehran")

    /** Tehran, rounded sample coordinates (not official data). */
    val TEHRAN: ChosenPlace =
        ChosenPlace(PlaceSource.COORDINATES, null, "Tehran", Coordinates(35.69, 51.42), "Asia/Tehran")

    /** Tromsø, rounded sample coordinates, for polar day and night. */
    val TROMSO: ChosenPlace =
        ChosenPlace(PlaceSource.COORDINATES, null, "Tromsø", Coordinates(69.65, 18.96), "Europe/Oslo")

    /** 1 Farvardin 1405 (Nowruz), 10:00 in Tehran. */
    val NOWRUZ_MORNING: Instant = Instant.parse("2026-03-21T06:30:00Z")

    val calculator: WearDayCalculator by lazy { WearDayCalculator(OfficialEvents.ALL) }

    fun preferences(
        language: String = "fa",
        place: ChosenPlace? = TEHRAN,
    ): UserPreferences = UserPreferences.defaultsFor(language).copy(place = place)

    fun setup(
        language: String = "fa",
        place: ChosenPlace? = TEHRAN,
        deviceZone: TimeZone = TEHRAN_ZONE,
    ): WearSetup = preferences(language, place).toWearSetup(deviceZone) { _, _ -> null }

    fun fixedClock(instant: Instant): Clock =
        object : Clock {
            override fun now(): Instant = instant
        }
}

/** Robolectric application whose graph uses a fixed clock at [WearFixtures.NOWRUZ_MORNING] and Tehran as the zone. */
class TestWearApplication :
    Application(),
    WearGraphOwner {
    override val graph: WearGraph by lazy {
        WearGraph(
            preferences =
                UserPreferencesRepository(
                    UserPreferencesRepository.createDataStore(this, CoroutineScope(SupervisorJob() + Dispatchers.IO)) {
                        "fa"
                    },
                ),
            clock = WearFixtures.fixedClock(WearFixtures.NOWRUZ_MORNING),
            deviceZone = { WearFixtures.TEHRAN_ZONE },
            cityCatalog = lazy { CityCatalog.loadBundled() },
            calculator = WearFixtures.calculator,
        )
    }
}
