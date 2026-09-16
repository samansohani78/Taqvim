/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/** An unsaved form and its typed date phrase, as restored after the process was recreated (B11). */
internal data class EditorDraft(
    val form: EditorForm,
    val dateText: String,
)

/**
 * The editing session as compact JSON for `SavedStateHandle` (B11): every field of [EditorForm] except the kept
 * recurrence rule, which only ever comes from the stored event and is taken from the reopened form instead.
 */
internal object EditorDraftCodec {
    private const val VERSION = 1

    /** Positions in the `[system, year, month, day]` array of a saved date. */
    private const val SYSTEM = 0
    private const val YEAR = 1
    private const val MONTH = 2
    private const val DAY = 3

    fun encode(
        eventId: Long?,
        form: EditorForm,
        dateText: String,
    ): String =
        buildJsonObject {
            put("v", VERSION)
            put("eventId", eventId)
            put("dateText", dateText)
            put("form", formJson(form))
        }.toString()

    /**
     * The draft saved for [eventId], using [opened] (the form as the event opens now) for the kept recurrence rule;
     * `null` for another event, another format or a draft that no longer fits its calendar.
     */
    fun decode(
        text: String,
        eventId: Long?,
        opened: EditorForm,
    ): EditorDraft? =
        runCatching {
            val root = Json.parseToJsonElement(text).jsonObject
            val savedId = root["eventId"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.long
            if (root.int("v") != VERSION || savedId != eventId) return null
            EditorDraft(form(root.getValue("form").jsonObject, opened), root.string("dateText"))
        }.getOrNull()

    private fun formJson(form: EditorForm): JsonObject =
        buildJsonObject {
            put("id", form.id)
            put("title", form.title)
            put("notes", form.notes)
            put("calendar", form.calendar.name)
            put("start", date(form.start))
            put("end", date(form.end))
            put("allDay", form.allDay)
            put("startMinute", form.startMinute)
            put("endMinute", form.endMinute)
            put("timeZoneId", form.timeZoneId)
            put("colorArgb", form.colorArgb)
            put("repeat", form.repeat?.let(::repeatJson) ?: JsonNull)
            put("reminders", buildJsonArray { form.reminderMinutes.forEach { add(JsonPrimitive(it)) } })
            put("sourceLink", form.sourceLink)
        }

    private fun repeatJson(repeat: RepeatForm): JsonObject =
        buildJsonObject {
            put("frequency", repeat.frequency.name)
            put("interval", repeat.intervalText)
            put("end", repeat.end.name)
            put("count", repeat.countText)
            put("until", date(repeat.until))
            put("weekdays", buildJsonArray { repeat.weekdays.forEach { add(JsonPrimitive(it.name)) } })
            put("invalidDates", repeat.invalidDates.name)
            put("keepsRule", repeat.keptRule != null)
        }

    private fun form(
        json: JsonObject,
        opened: EditorForm,
    ): EditorForm =
        EditorForm(
            id = json["id"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.long,
            title = json.string("title"),
            notes = json.string("notes"),
            calendar = CalendarSystem.valueOf(json.string("calendar")),
            start = date(json.getValue("start")),
            end = date(json.getValue("end")),
            allDay = json.getValue("allDay").jsonPrimitive.boolean,
            startMinute = json.int("startMinute"),
            endMinute = json.int("endMinute"),
            timeZoneId = json.string("timeZoneId"),
            colorArgb = json["colorArgb"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.int,
            repeat = json["repeat"]?.takeUnless { it is JsonNull }?.let { repeat(it.jsonObject, opened) },
            reminderMinutes = json.getValue("reminders").jsonArray.map { it.jsonPrimitive.int },
            sourceLink = json.string("sourceLink"),
        )

    private fun repeat(
        json: JsonObject,
        opened: EditorForm,
    ): RepeatForm =
        RepeatForm(
            frequency = Frequency.valueOf(json.string("frequency")),
            intervalText = json.string("interval"),
            end = RecurrenceEnd.valueOf(json.string("end")),
            countText = json.string("count"),
            until = date(json.getValue("until")),
            weekdays =
                json
                    .getValue("weekdays")
                    .jsonArray
                    .map { Weekday.valueOf(it.jsonPrimitive.content) }
                    .toSet(),
            invalidDates = InvalidDatePolicy.valueOf(json.string("invalidDates")),
            keptRule = opened.repeat?.keptRule?.takeIf { json.getValue("keepsRule").jsonPrimitive.boolean },
        )

    private fun date(date: CalendarDate): JsonArray =
        buildJsonArray {
            add(JsonPrimitive(date.system.name))
            add(JsonPrimitive(date.year))
            add(JsonPrimitive(date.month))
            add(JsonPrimitive(date.day))
        }

    private fun date(json: JsonElement): CalendarDate {
        val parts = json.jsonArray.map { it.jsonPrimitive }
        return CalendarDate(
            CalendarSystem.valueOf(parts[SYSTEM].content),
            parts[YEAR].int,
            parts[MONTH].int,
            parts[DAY].int,
        )
    }

    private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content

    private fun JsonObject.int(key: String): Int = getValue(key).jsonPrimitive.int
}
