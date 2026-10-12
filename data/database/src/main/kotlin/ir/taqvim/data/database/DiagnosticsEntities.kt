/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Severity of a diagnostics entry. */
enum class DiagnosticsLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

/** One local diagnostics entry (`diagnostics_log`), a ring of at most [DiagnosticsDao.MAX_ROWS] rows. */
@Entity(tableName = "diagnostics_log")
data class DiagnosticsLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "at_epoch_millis") val atEpochMillis: Long,
    val level: DiagnosticsLevel,
    val tag: String,
    val message: String,
)
