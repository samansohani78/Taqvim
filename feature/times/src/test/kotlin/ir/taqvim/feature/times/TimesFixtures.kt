/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.times

import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.praytimes.PrayerSettings
import ir.taqvim.core.testing.TimeZones
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/** Sample places and instants for the Times tab tests (sample coordinates, not official data). */
object TimesFixtures {
    fun language(code: String): LanguageSpec = requireNotNull(LanguageTable.forCode(code)) { "no language $code" }

    fun tehran(languageCode: String = "en"): TimesSettings =
        TimesSettings(
            placeName = "Tehran",
            place = Coordinates(35.69, 51.39),
            timeZone = TimeZones.TEHRAN,
            prayer = PrayerSettings(),
            language = language(languageCode),
        )

    /** Tromsø: the Sun does not set around the June solstice. */
    fun tromso(): TimesSettings =
        TimesSettings(
            placeName = "Tromso",
            place = Coordinates(69.65, 18.96),
            timeZone = TimeZone.of("Europe/Oslo"),
            prayer = PrayerSettings(),
            language = language("en"),
        )

    /** [isoLocal] (e.g. `2026-06-21T12:00`) in the zone of [settings]. */
    fun at(
        isoLocal: String,
        settings: TimesSettings,
    ): Instant = LocalDateTime.parse(isoLocal).toInstant(settings.timeZone)
}
