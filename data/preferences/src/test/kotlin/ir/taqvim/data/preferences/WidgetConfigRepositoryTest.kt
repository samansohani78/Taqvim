/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.preferences

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.data.preferences.proto.WidgetConfigsProto
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** T-1200 widget configuration store: per widget id, round trips, deletion and unreadable files. */
class WidgetConfigRepositoryTest {
    private val sample =
        StoredWidgetConfig(
            background = "PRIMARY_CONTAINER",
            transparencyPercent = 30,
            scalePercent = 125,
            contents = setOf("WEEKDAY", "EVENTS"),
            secondaryCalendar = CalendarSystem.GREGORIAN,
        )

    @Test
    fun `configurations are stored per widget id and survive a round trip`(): Unit =
        runTest {
            val repository = WidgetConfigRepository(InMemoryWidgetConfigs())

            repository.config(1).shouldBeNull()
            repository.save(1, sample)
            repository.save(2, sample.copy(secondaryCalendar = null, contents = emptySet()))

            repository.config(1) shouldBe sample
            repository.config(2) shouldBe sample.copy(secondaryCalendar = null, contents = emptySet())
            repository.save(1, sample.copy(scalePercent = 75))
            repository.config(1) shouldBe sample.copy(scalePercent = 75)
        }

    @Test
    fun `countdown dates survive a round trip and unknown calendars drop them`(): Unit =
        runTest {
            val countdown =
                StoredCountdown(
                    title = "Nowruz",
                    calendar = CalendarSystem.PERSIAN,
                    year = 1403,
                    month = 12,
                    day = 30,
                    mode = "SINCE",
                    repeatsYearly = true,
                    startJdn = 2_461_297L,
                )
            val repository = WidgetConfigRepository(InMemoryWidgetConfigs())
            repository.save(5, sample.copy(countdown = countdown))

            repository.config(5) shouldBe sample.copy(countdown = countdown)
            val proto = sample.copy(countdown = countdown).toProto()
            proto.hasCountdown() shouldBe true
            sample.toProto().hasCountdown() shouldBe false
            val unknown =
                proto
                    .toBuilder()
                    .setCountdown(proto.countdown.toBuilder().setCalendarValue(UNKNOWN_CALENDAR))
                    .build()
            unknown.toStored().countdown.shouldBeNull()
        }

    @Test
    fun `deleted widgets are forgotten and unknown ids ignored`(): Unit =
        runTest {
            val repository = WidgetConfigRepository(InMemoryWidgetConfigs())
            repository.save(1, sample)
            repository.save(2, sample)

            repository.delete(setOf(1, 99))
            repository.delete(emptySet())

            repository.config(1).shouldBeNull()
            repository.config(2) shouldBe sample
        }

    @Test
    fun `the serializer round-trips and reports unreadable files`(): Unit =
        runTest {
            val proto = WidgetConfigsProto.newBuilder().putConfigs(4, sample.toProto()).build()
            val bytes = ByteArrayOutputStream().also { WidgetConfigsSerializer.writeTo(proto, it) }.toByteArray()

            val read = WidgetConfigsSerializer.readFrom(ByteArrayInputStream(bytes))
            read.configsMap.getValue(4).toStored() shouldBe sample
            WidgetConfigsSerializer.defaultValue.configsMap.isEmpty() shouldBe true
            // Field 1 with wire type 7, which does not exist.
            val garbage = byteArrayOf(0x0F)
            shouldThrow<CorruptionException> { WidgetConfigsSerializer.readFrom(ByteArrayInputStream(garbage)) }
        }
}

/** A calendar enum number that no release defines. */
private const val UNKNOWN_CALENDAR = 99

private class InMemoryWidgetConfigs : DataStore<WidgetConfigsProto> {
    private val state = MutableStateFlow(WidgetConfigsProto.getDefaultInstance())

    override val data: Flow<WidgetConfigsProto> = state

    override suspend fun updateData(
        transform: suspend (t: WidgetConfigsProto) -> WidgetConfigsProto,
    ): WidgetConfigsProto = transform(state.value).also { state.value = it }
}
