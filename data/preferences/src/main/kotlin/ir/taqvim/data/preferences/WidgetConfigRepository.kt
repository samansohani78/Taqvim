/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.preferences

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.data.preferences.proto.CalendarSystemProto
import ir.taqvim.data.preferences.proto.WidgetConfigProto
import ir.taqvim.data.preferences.proto.WidgetConfigsProto
import ir.taqvim.data.preferences.proto.WidgetCountdownProto
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first

/**
 * A placed widget's stored settings, independent of the widget module's types: [background] and [contents] are the
 * names of the widget enums, [secondaryCalendar] is `null` for the language's second calendar, and [countdown] is the
 * countdown widget's date (T-1212).
 */
data class StoredWidgetConfig(
    val background: String,
    val transparencyPercent: Int,
    val scalePercent: Int,
    val contents: Set<String>,
    val secondaryCalendar: CalendarSystem?,
    val countdown: StoredCountdown? = null,
)

/**
 * A countdown widget's stored date (T-1212): [year]-[month]-[day] in [calendar], [mode] the widget module's
 * `CountdownMode` name, and [startJdn] the Julian day number of the day it was chosen.
 */
data class StoredCountdown(
    val title: String,
    val calendar: CalendarSystem,
    val year: Int,
    val month: Int,
    val day: Int,
    val mode: String,
    val repeatsYearly: Boolean,
    val startJdn: Long,
)

/** Proto serializer of [WidgetConfigsProto]; unreadable files surface as [CorruptionException]. */
object WidgetConfigsSerializer : Serializer<WidgetConfigsProto> {
    override val defaultValue: WidgetConfigsProto = WidgetConfigsProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): WidgetConfigsProto =
        runCatching { WidgetConfigsProto.parseFrom(input) }
            .getOrElse { throw CorruptionException("WidgetConfigs could not be read", it) }

    override suspend fun writeTo(
        t: WidgetConfigsProto,
        output: OutputStream,
    ) = t.writeTo(output)
}

/** Per-widget configurations keyed by app widget id (T-1200), stored in their own Proto DataStore file. */
class WidgetConfigRepository(
    private val dataStore: DataStore<WidgetConfigsProto>,
) {
    /** The configuration of [appWidgetId], or `null` when it was never saved. */
    suspend fun config(appWidgetId: Int): StoredWidgetConfig? =
        dataStore.data
            .first()
            .configsMap[appWidgetId]
            ?.toStored()

    suspend fun save(
        appWidgetId: Int,
        config: StoredWidgetConfig,
    ) {
        dataStore.updateData { it.toBuilder().putConfigs(appWidgetId, config.toProto()).build() }
    }

    /** Forgets [appWidgetIds]; unknown ids are ignored. */
    suspend fun delete(appWidgetIds: Set<Int>) {
        if (appWidgetIds.isEmpty()) return
        dataStore.updateData { stored ->
            appWidgetIds.fold(stored.toBuilder()) { builder, id -> builder.removeConfigs(id) }.build()
        }
    }

    companion object {
        /** File name under `files/datastore/`. */
        const val FILE_NAME: String = "widget_configs.pb"

        /** The widget configuration store; create one per process and keep it for the app's lifetime. */
        fun createDataStore(
            context: Context,
            scope: CoroutineScope,
        ): DataStore<WidgetConfigsProto> =
            DataStoreFactory.create(
                serializer = WidgetConfigsSerializer,
                scope = scope,
                produceFile = { context.dataStoreFile(FILE_NAME) },
            )
    }
}

private const val CALENDAR_PREFIX = "CALENDAR_SYSTEM_"

internal fun WidgetConfigProto.toStored(): StoredWidgetConfig =
    StoredWidgetConfig(
        background = background,
        transparencyPercent = transparencyPercent,
        scalePercent = scalePercent,
        contents = contentsList.toSet(),
        secondaryCalendar = calendarOf(secondaryCalendar),
        countdown = if (hasCountdown()) countdown.toStored() else null,
    )

internal fun StoredWidgetConfig.toProto(): WidgetConfigProto =
    WidgetConfigProto
        .newBuilder()
        .setBackground(background)
        .setTransparencyPercent(transparencyPercent)
        .setScalePercent(scalePercent)
        .addAllContents(contents.sorted())
        .setSecondaryCalendar(protoOf(secondaryCalendar))
        .also { builder -> countdown?.let { builder.setCountdown(it.toProto()) } }
        .build()

/** The stored countdown, or `null` when its calendar is unknown. */
private fun WidgetCountdownProto.toStored(): StoredCountdown? {
    val system = calendarOf(calendar) ?: return null
    return StoredCountdown(title, system, year, month, day, mode, repeatsYearly, startJdn)
}

private fun StoredCountdown.toProto(): WidgetCountdownProto =
    WidgetCountdownProto
        .newBuilder()
        .setTitle(title)
        .setCalendar(protoOf(calendar))
        .setYear(year)
        .setMonth(month)
        .setDay(day)
        .setMode(mode)
        .setRepeatsYearly(repeatsYearly)
        .setStartJdn(startJdn)
        .build()

private fun calendarOf(proto: CalendarSystemProto): CalendarSystem? =
    CalendarSystem.entries.firstOrNull { CALENDAR_PREFIX + it.name == proto.name }

private fun protoOf(calendar: CalendarSystem?): CalendarSystemProto =
    calendar?.let { CalendarSystemProto.valueOf(CALENDAR_PREFIX + it.name) }
        ?: CalendarSystemProto.CALENDAR_SYSTEM_UNSPECIFIED
