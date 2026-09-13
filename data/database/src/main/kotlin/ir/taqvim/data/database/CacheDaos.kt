/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** iCalendar subscriptions and their cached occurrences. */
@Dao
interface IcsSubscriptionDao {
    @Insert
    suspend fun insertSubscription(subscription: IcsSubscriptionEntity): Long

    @Update
    suspend fun updateSubscription(subscription: IcsSubscriptionEntity)

    /** Deletes the subscription with its cached occurrences. */
    @Query("DELETE FROM ics_subscriptions WHERE id = :id")
    suspend fun deleteSubscription(id: Long)

    @Query("SELECT * FROM ics_subscriptions WHERE id = :id")
    suspend fun getSubscription(id: Long): IcsSubscriptionEntity?

    @Query("SELECT * FROM ics_subscriptions ORDER BY display_name, id")
    fun observeSubscriptions(): Flow<List<IcsSubscriptionEntity>>

    @Insert
    suspend fun insertEvents(events: List<IcsEventCacheEntity>)

    @Query("DELETE FROM ics_events_cache WHERE subscription_id = :subscriptionId")
    suspend fun deleteEvents(subscriptionId: Long)

    /** Atomically replaces the cached occurrences of [subscriptionId] with [events]. */
    @Transaction
    suspend fun replaceEvents(
        subscriptionId: Long,
        events: List<IcsEventCacheEntity>,
    ) {
        require(events.all { it.subscriptionId == subscriptionId }) { "events must belong to $subscriptionId" }
        deleteEvents(subscriptionId)
        insertEvents(events)
    }

    /** Cached occurrences of enabled subscriptions overlapping `[fromEpochMillis, toEpochMillis)`. */
    @Query(
        """
        SELECT ics_events_cache.* FROM ics_events_cache
        JOIN ics_subscriptions ON ics_subscriptions.id = ics_events_cache.subscription_id
        WHERE ics_subscriptions.enabled
            AND ics_events_cache.start_epoch_millis < :toEpochMillis
            AND ics_events_cache.end_epoch_millis > :fromEpochMillis
        ORDER BY ics_events_cache.start_epoch_millis, ics_events_cache.uid
        """,
    )
    fun observeEvents(
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): Flow<List<IcsEventCacheEntity>>
}

/** Instances copied from the device calendar provider. */
@Dao
interface DeviceEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<DeviceEventCacheEntity>)

    @Query(
        """
        DELETE FROM device_events_cache
        WHERE begin_epoch_millis >= :fromEpochMillis AND begin_epoch_millis < :toEpochMillis
        """,
    )
    suspend fun deleteWindow(
        fromEpochMillis: Long,
        toEpochMillis: Long,
    )

    /** Atomically replaces the instances beginning in `[fromEpochMillis, toEpochMillis)` with [events]. */
    @Transaction
    suspend fun replaceWindow(
        fromEpochMillis: Long,
        toEpochMillis: Long,
        events: List<DeviceEventCacheEntity>,
    ) {
        deleteWindow(fromEpochMillis, toEpochMillis)
        insertAll(events)
    }

    /** Instances overlapping `[fromEpochMillis, toEpochMillis)`. */
    @Query(
        """
        SELECT * FROM device_events_cache
        WHERE begin_epoch_millis < :toEpochMillis AND end_epoch_millis > :fromEpochMillis
        ORDER BY begin_epoch_millis, event_id
        """,
    )
    fun observeInRange(
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): Flow<List<DeviceEventCacheEntity>>
}

/** The local diagnostics ring buffer. */
@Dao
interface DiagnosticsDao {
    @Insert
    suspend fun insert(entry: DiagnosticsLogEntity): Long

    /** Deletes all but the newest [keep] entries. */
    @Query(
        """
        DELETE FROM diagnostics_log
        WHERE id <= (SELECT id FROM diagnostics_log ORDER BY id DESC LIMIT 1 OFFSET :keep)
        """,
    )
    suspend fun trimTo(keep: Int)

    /** Appends [entry] and drops the oldest entries beyond [MAX_ROWS]. */
    @Transaction
    suspend fun append(entry: DiagnosticsLogEntity) {
        insert(entry)
        trimTo(MAX_ROWS)
    }

    /** The newest [limit] entries, newest first. */
    @Query("SELECT * FROM diagnostics_log ORDER BY id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DiagnosticsLogEntity>>

    @Query("SELECT COUNT(*) FROM diagnostics_log")
    suspend fun count(): Int

    @Query("DELETE FROM diagnostics_log")
    suspend fun clear()

    companion object {
        /** Ring capacity (docs/PLAN.md §4.3). */
        const val MAX_ROWS: Int = 2_000
    }
}
