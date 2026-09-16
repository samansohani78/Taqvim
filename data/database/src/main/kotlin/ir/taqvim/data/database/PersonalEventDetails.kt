/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database

import androidx.room.Embedded
import androidx.room.Relation

/**
 * A personal event with its recurrence rule, exception days and overridden occurrences (T-1003). Room reads the
 * children in the same transaction as the events and loads each relation for all returned events at once, so a list
 * costs a fixed number of queries and every child matches its parent row (review I02). Children are in no particular
 * order.
 */
data class PersonalEventDetails(
    @Embedded val event: PersonalEventEntity,
    @Relation(parentColumn = "id", entityColumn = "event_id")
    val recurrence: EventRecurrenceEntity?,
    @Relation(parentColumn = "id", entityColumn = "event_id")
    val exceptions: List<EventExceptionEntity>,
    @Relation(parentColumn = "id", entityColumn = "event_id")
    val overrides: List<EventOverrideEntity>,
)

/** [PersonalEventDetails] with the event's reminders (T-1001), read the same way; children are unordered. */
data class PersonalEventReminderDetails(
    @Embedded val event: PersonalEventEntity,
    @Relation(parentColumn = "id", entityColumn = "event_id")
    val recurrence: EventRecurrenceEntity?,
    @Relation(parentColumn = "id", entityColumn = "event_id")
    val exceptions: List<EventExceptionEntity>,
    @Relation(parentColumn = "id", entityColumn = "event_id")
    val overrides: List<EventOverrideEntity>,
    @Relation(parentColumn = "id", entityColumn = "event_id")
    val reminders: List<ReminderEntity>,
)
