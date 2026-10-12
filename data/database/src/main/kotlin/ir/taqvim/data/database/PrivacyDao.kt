/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** How many rows of each kind of personal or cached data are stored (privacy dashboard, T-1503). */
data class StoredRowCounts(
    val personalEvents: Int,
    /** Event reminders and reminders before official events. */
    val reminders: Int,
    val subscriptions: Int,
    val diagnostics: Int,
)

/** Row counts for the privacy dashboard (T-1503); each kind is cleared through the DAO that owns it. */
@Dao
interface PrivacyDao {
    /** The current counts; emits again whenever one of the counted tables changes. */
    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM personal_events) AS personalEvents,
            (SELECT COUNT(*) FROM reminders) + (SELECT COUNT(*) FROM official_reminders) AS reminders,
            (SELECT COUNT(*) FROM ics_subscriptions) AS subscriptions,
            (SELECT COUNT(*) FROM diagnostics_log) AS diagnostics
        """,
    )
    fun observeCounts(): Flow<StoredRowCounts>
}
