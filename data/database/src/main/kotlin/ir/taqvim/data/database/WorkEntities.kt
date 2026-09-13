/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.workdays.HalfDayPolicy
import ir.taqvim.core.workdays.LeaveRange
import ir.taqvim.core.workdays.WorkdayProfile

/** A repeating shift cycle (`shift_rotations`): [pattern] holds one shift label per day, starting on [anchorJdn]. */
@Entity(tableName = "shift_rotations")
data class ShiftRotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "anchor_jdn") val anchorJdn: Long,
    val pattern: List<String>,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
)

/** The shift actually worked on day [jdn] of a rotation (`shift_rotation_records`), overriding the pattern. */
@Entity(
    tableName = "shift_rotation_records",
    primaryKeys = ["rotation_id", "jdn"],
    foreignKeys = [
        ForeignKey(
            entity = ShiftRotationEntity::class,
            parentColumns = ["id"],
            childColumns = ["rotation_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ShiftRotationRecordEntity(
    @ColumnInfo(name = "rotation_id") val rotationId: Long,
    val jdn: Long,
    val shift: String,
    val note: String = "",
)

/** A named [WorkdayProfile] (`workday_profiles`); at most one should be [isDefault]. */
@Entity(tableName = "workday_profiles")
data class WorkdayProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val weekend: Set<Weekday>,
    @ColumnInfo(name = "holiday_sources") val holidaySources: Set<EventSource>,
    @ColumnInfo(name = "half_days") val halfDays: HalfDayPolicy,
    @ColumnInfo(name = "personal_leave") val personalLeave: List<LeaveRange>,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
)

/** The stored profile as a [WorkdayProfile] (T-504). */
fun WorkdayProfileEntity.toProfile(): WorkdayProfile = WorkdayProfile(weekend, holidaySources, halfDays, personalLeave)
