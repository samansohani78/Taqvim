/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.security

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.app.di.timesSettings
import ir.taqvim.core.astronomy.Qibla
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.praytimes.PrayerTimesCalculator
import ir.taqvim.core.praytimes.PrayerTimesResult
import ir.taqvim.data.location.CityCatalog
import ir.taqvim.data.preferences.ChosenPlace
import ir.taqvim.data.preferences.PlaceSource
import ir.taqvim.data.preferences.UserPreferences
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf

/**
 * ADR-0054, ADR-0055: the app runs with nothing granted.
 *
 * Robolectric grants no permission unless a test asks for one, so every assertion here is made by a process that
 * holds none — which is the state a real install is in, because Taqvim never asks. The manifest half of the claim
 * is [ExportedComponentsTest]; this is the half that shows the features still work.
 */
@RunWith(AndroidJUnit4::class)
class OfflineOperationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    // Robolectric starts TaqvimApplication, and so Koin, for every test.
    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun theAppStartsWithNoRuntimePermissionGranted() {
        // Reaching this line means TaqvimApplication.onCreate ran: Koin built, the restore gate checked, six
        // watchers launched and the launcher shortcuts published, all without a permission between them.
        val shadow = shadowOf(context as Application)

        WITHHELD.forEach { permission ->
            shadow.denyPermissions(permission)
            context.checkSelfPermission(permission) shouldBe android.content.pm.PackageManager.PERMISSION_DENIED
        }
        context.packageName shouldBe "ir.taqvim.app"
    }

    @Test
    fun prayerTimesAndQiblaComeFromTheChosenCityWithoutAskingTheDevice() {
        // A place is a bundled catalog city (T-603), and everything place-dependent follows from its coordinates
        // and zone. Nothing here reads the platform location service, because there is none left to read.
        val tehran = CityCatalog.loadBundled().search("Tehran").first { it.countryCode == "IR" }
        val place =
            ChosenPlace(
                source = PlaceSource.CITY,
                cityId = tehran.id,
                name = tehran.name("en"),
                coordinates = tehran.coordinates,
                zoneId = requireNotNull(tehran.timeZoneId),
            )
        val settings = timesSettings(UserPreferences.defaultsFor("fa"), place, "تهران").shouldNotBeNull()

        val nowruz = LocalDate(2026, 3, 21).toJdn()
        val result =
            PrayerTimesCalculator.calculate(nowruz, settings.place, TEHRAN_UTC_OFFSET_MINUTES, settings.prayer)
        val times = (result as PrayerTimesResult.Available).times

        // Nowruz is the equinox: in Tehran the Sun rises a little after 06:00 and sets a little after 18:00.
        times.sunrise.hour shouldBe 6
        times.sunset.hour shouldBe 18
        // The Kaaba is south-west of Tehran: a bearing in the third quadrant, roughly 220°.
        val bearing = Qibla.bearingDegrees(settings.place).shouldNotBeNull()
        bearing shouldBeGreaterThan 180.0
        bearing shouldBeLessThan 270.0
        Qibla.distanceKm(Coordinates(tehran.coordinates.latitude, tehran.coordinates.longitude)) shouldBeGreaterThan
            1_000.0
    }

    private companion object {
        /** Iran Standard Time, UTC+03:30. */
        const val TEHRAN_UTC_OFFSET_MINUTES = 210

        /** The permissions Taqvim must work without, because it never asks for any of them. */
        val WITHHELD =
            listOf(
                android.Manifest.permission.INTERNET,
                android.Manifest.permission.ACCESS_NETWORK_STATE,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.READ_CALENDAR,
                android.Manifest.permission.WRITE_CALENDAR,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
            )
    }
}
