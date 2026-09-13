/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.events.CalendarProvider
import ir.taqvim.core.ics.IcsCalendar
import ir.taqvim.core.ics.IcsParseResult
import ir.taqvim.core.ics.IcsProblem
import ir.taqvim.core.ics.IcsReader
import ir.taqvim.core.ics.IcsWriter
import ir.taqvim.data.database.PersonalEventDao
import ir.taqvim.data.database.ReminderDao
import ir.taqvim.data.database.ReminderEntity
import ir.taqvim.data.database.toEntity
import ir.taqvim.data.database.toRule
import kotlin.time.Clock
import kotlinx.datetime.TimeZone

/** Runs database work atomically. */
interface TransactionRunner {
    suspend fun inTransaction(block: suspend () -> Unit)
}

/** [TransactionRunner] over a Room database transaction. */
class RoomTransactionRunner(
    private val database: RoomDatabase,
) : TransactionRunner {
    override suspend fun inTransaction(block: suspend () -> Unit) {
        database.withTransaction { block() }
    }
}

/** What to do with an imported event whose UID is already stored. */
enum class DuplicatePolicy {
    /** Keep the stored event. */
    SKIP,

    /** Overwrite the stored event, its rule and its reminders (keeping its color and creation time). */
    REPLACE,
}

/** Outcome of an iCalendar import. */
sealed interface ImportResult {
    /** Events [created], [replaced] and [skipped] (repeated UIDs included), [warnings] and the reader's [problems]. */
    data class Imported(
        val created: Int,
        val replaced: Int,
        val skipped: Int,
        val warnings: List<ImportWarning>,
        val problems: List<IcsProblem>,
    ) : ImportResult

    /** The text is not a readable iCalendar stream; nothing was stored. */
    data class Unreadable(
        val problems: List<IcsProblem>,
    ) : ImportResult
}

/** Imports iCalendar text as personal events in one transaction, matching stored events by UID (T-1003). */
class IcsImporter(
    private val events: PersonalEventDao,
    private val reminders: ReminderDao,
    private val transactions: TransactionRunner,
    private val clock: Clock,
    private val zone: () -> TimeZone,
) {
    private enum class Stored { CREATED, REPLACED, SKIPPED }

    suspend fun import(
        text: String,
        duplicates: DuplicatePolicy = DuplicatePolicy.REPLACE,
    ): ImportResult =
        when (val parsed = IcsReader.read(text)) {
            is IcsParseResult.Failure -> ImportResult.Unreadable(parsed.errors)
            is IcsParseResult.Success -> store(parsed, duplicates)
        }

    private suspend fun store(
        parsed: IcsParseResult.Success,
        duplicates: DuplicatePolicy,
    ): ImportResult.Imported {
        val mapping = IcsEventMapping(zone())
        val now = clock.now().toEpochMilliseconds()
        val unique = parsed.calendar.events.distinctBy { it.uid }
        val imported = unique.map { mapping.toImported(it, now) }
        val outcomes = mutableListOf<Stored>()
        transactions.inTransaction { imported.forEach { outcomes += storeOne(it, duplicates) } }
        return ImportResult.Imported(
            created = outcomes.count { it == Stored.CREATED },
            replaced = outcomes.count { it == Stored.REPLACED },
            skipped = outcomes.count { it == Stored.SKIPPED } + parsed.calendar.events.size - unique.size,
            warnings = imported.flatMap { it.warnings },
            problems = parsed.warnings,
        )
    }

    private suspend fun storeOne(
        item: ImportedEvent,
        duplicates: DuplicatePolicy,
    ): Stored {
        val existing = item.event.icsUid?.let { events.getByIcsUid(it) }
        return when {
            existing == null -> {
                saveDetails(events.insert(item.event), item)
                Stored.CREATED
            }

            duplicates == DuplicatePolicy.SKIP -> {
                Stored.SKIPPED
            }

            else -> {
                events.update(
                    item.event.copy(
                        id = existing.id,
                        colorArgb = existing.colorArgb,
                        createdAtEpochMillis = existing.createdAtEpochMillis,
                    ),
                )
                reminders.deleteReminders(existing.id)
                saveDetails(existing.id, item)
                Stored.REPLACED
            }
        }
    }

    private suspend fun saveDetails(
        eventId: Long,
        item: ImportedEvent,
    ) {
        val rule = item.recurrence
        if (rule == null) {
            events.deleteRecurrence(eventId)
        } else {
            events.upsertRecurrence(rule.toEntity(eventId, item.event.calendarSystem))
        }
        item.reminderMinutes.forEach { reminders.insertReminder(ReminderEntity(eventId = eventId, minutesBefore = it)) }
    }
}

/** Exports personal events as iCalendar text (T-1003, ADR-0013). */
class IcsExporter(
    private val events: PersonalEventDao,
    private val reminders: ReminderDao,
    private val calendars: suspend () -> CalendarProvider,
    private val clock: Clock,
    private val zone: () -> TimeZone,
    private val limits: ExportLimits = ExportLimits(),
) {
    /**
     * Every personal event, or only those with [ids]. An event without a UID gets one, which is stored so that
     * importing the file back matches the event instead of duplicating it.
     */
    suspend fun export(ids: Set<Long>? = null): String {
        val now = clock.now()
        val today = now.toJdn(zone())
        val mapping = IcsExportMapping(calendars(), limits)
        val icsEvents =
            events.all().filter { ids == null || it.id in ids }.map { stored ->
                val event = stored.takeIf { it.icsUid != null } ?: stored.copy(icsUid = uidOf(stored))
                if (stored.icsUid == null) events.update(event)
                val record =
                    ExportRecord(event, events.getRecurrence(event.id)?.toRule(), reminders.reminders(event.id))
                mapping.toIcs(record, today)
            }
        return IcsWriter.write(IcsCalendar(PRODUCT_ID, icsEvents), now)
    }

    companion object {
        /** PRODID of exported calendars (RFC 5545 §3.7.3). */
        const val PRODUCT_ID: String = "-//Taqvim//Taqvim Calendar//EN"
    }
}
