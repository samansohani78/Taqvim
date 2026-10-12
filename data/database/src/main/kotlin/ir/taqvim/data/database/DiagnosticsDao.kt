/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

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
