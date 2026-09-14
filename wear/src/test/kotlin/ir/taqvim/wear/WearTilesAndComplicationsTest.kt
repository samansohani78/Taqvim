/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

/** T-1600: the month and next tiles and the three complications on 1 Farvardin 1405 in Tehran, in Persian. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fa")
class WearTilesAndComplicationsTest {
    private val app: TestWearApplication = ApplicationProvider.getApplicationContext()
    private val today by lazy { WearFixtures.calculator.today(WearFixtures.setup(), WearFixtures.NOWRUZ_MORNING) }

    @Before
    fun storeTehranPreferences(): Unit =
        runBlocking {
            app.graph.preferences.update { WearFixtures.preferences() }
        }

    @Test
    fun `month tile shows the month, weekday initials and every day`(): Unit =
        runBlocking {
            val texts = tileTexts(Robolectric.buildService(MonthTileService::class.java).create().get())

            texts shouldContain "فروردین ۱۴۰۵"
            texts shouldContain "۱"
            texts shouldContain "۳۱"
        }

    @Test
    fun `next tile shows today, the next prayer and the next occasion`(): Unit =
        runBlocking {
            val texts = tileTexts(Robolectric.buildService(NextTileService::class.java).create().get())
            val next = today.nextPrayer.shouldNotBeNull()

            texts.first() shouldBe today.primaryDate
            texts shouldContain
                app.getString(R.string.wear_next_prayer_at, app.getString(R.string.wear_prayer_dhuhr), next.clock)
            texts.size shouldBe 3
        }

    @Test
    fun `next tile asks for a city when none is chosen`(): Unit =
        runBlocking {
            app.graph.preferences.update { WearFixtures.preferences(place = null) }
            val texts = tileTexts(Robolectric.buildService(NextTileService::class.java).create().get())

            texts shouldContain app.getString(R.string.wear_no_place)
        }

    @Test
    fun `date complication is the day titled with the month`(): Unit =
        runBlocking {
            val service = Robolectric.buildService(DateComplicationService::class.java).create().get()
            val data = service.onComplicationRequest(request(ComplicationType.SHORT_TEXT))

            val text = data.shouldBeInstanceOf<ShortTextComplicationData>()
            text.text.getTextAt(app.resources, java.time.Instant.now()) shouldBe "۱"
            text.title.shouldNotBeNull().getTextAt(app.resources, java.time.Instant.now()) shouldBe "فروردین"
            service.getPreviewData(ComplicationType.SHORT_TEXT).shouldNotBeNull()
        }

    @Test
    fun `month progress complication is the day out of the month length`(): Unit =
        runBlocking {
            val service = Robolectric.buildService(MonthProgressComplicationService::class.java).create().get()
            val data = service.onComplicationRequest(request(ComplicationType.RANGED_VALUE))

            val ranged = data.shouldBeInstanceOf<RangedValueComplicationData>()
            ranged.value shouldBe 1f
            ranged.min shouldBe 0f
            ranged.max shouldBe 31f
            service.getPreviewData(ComplicationType.RANGED_VALUE).shouldNotBeNull()
        }

    @Test
    fun `next prayer complication shows the clock, or no data without a city`(): Unit =
        runBlocking {
            val service = Robolectric.buildService(NextPrayerComplicationService::class.java).create().get()
            val data = service.onComplicationRequest(request(ComplicationType.SHORT_TEXT))

            val text = data.shouldBeInstanceOf<ShortTextComplicationData>()
            text.text.getTextAt(app.resources, java.time.Instant.now()) shouldBe
                today.nextPrayer.shouldNotBeNull().clock
            service.getPreviewData(ComplicationType.SHORT_TEXT).shouldNotBeNull()

            app.graph.preferences.update { WearFixtures.preferences(place = null) }
            service
                .onComplicationRequest(
                    request(ComplicationType.SHORT_TEXT),
                ).shouldBeInstanceOf<NoDataComplicationData>()
        }

    private suspend fun tileTexts(service: TaqvimTileService): List<String> {
        val tile = service.currentTile()
        tile.freshnessIntervalMillis shouldBe TileLayouts.MINUTE_MILLIS.coerceAtLeast(tile.freshnessIntervalMillis)
        val entry =
            tile.tileTimeline
                .shouldNotBeNull()
                .timelineEntries
                .single()
        return TileLayouts.texts(entry.layout?.root)
    }

    private fun request(type: ComplicationType) = ComplicationRequest(1, type, false)
}
