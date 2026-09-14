/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.data.location.CityCatalog
import ir.taqvim.data.preferences.PlaceSource
import org.junit.jupiter.api.Test

/** T-1600: settings chosen on the watch are stored as ordinary preferences (ADR-0019). */
class WearSettingsModelTest {
    private val catalog by lazy { CityCatalog.loadBundled() }

    @Test
    fun `the chosen main calendar comes first and the others keep their order`() {
        val stored = WearFixtures.preferences()
        val changed = WearSettingsModel.withPrimaryCalendar(stored, CalendarSystem.GREGORIAN)

        changed.calendars.first() shouldBe CalendarSystem.GREGORIAN
        changed.calendars.drop(1) shouldBe stored.calendars - CalendarSystem.GREGORIAN
        WearSettingsModel.withPrimaryCalendar(changed, CalendarSystem.GREGORIAN) shouldBe changed
    }

    @Test
    fun `language and prayer method are stored as chosen`() {
        val stored = WearFixtures.preferences()

        WearSettingsModel.withLanguage(stored, "en").languageCode shouldBe "en"
        WearSettingsModel.withPrayerMethod(stored, PrayerMethod.MWL).prayerMethod shouldBe PrayerMethod.MWL
    }

    @Test
    fun `a catalog city becomes the place, named in the watch language`() {
        val tehran = catalog.search("Tehran").first()
        val stored = WearSettingsModel.withCity(WearFixtures.preferences(place = null), tehran)

        val place = stored.place.shouldNotBeNull()
        place.source shouldBe PlaceSource.CITY
        place.cityId shouldBe tehran.id
        place.zoneId shouldBe tehran.timeZoneId
        val setup = stored.toWearSetup(WearFixtures.TEHRAN_ZONE) { id, code -> catalog.city(id)?.name(code) }
        setup.place.shouldNotBeNull().name shouldBe tehran.name("fa")
    }

    @Test
    fun `city options are the most populous cities that have a time zone`() {
        val options = WearSettingsModel.cityOptions(catalog, "en")

        options shouldHaveSize WearSettingsModel.CITY_OPTIONS
        val cities = options.map { requireNotNull(catalog.city(it.id)) }
        cities.all { it.timeZoneId != null } shouldBe true
        cities.map { it.population ?: 0L } shouldBe cities.map { it.population ?: 0L }.sortedDescending()
    }
}
