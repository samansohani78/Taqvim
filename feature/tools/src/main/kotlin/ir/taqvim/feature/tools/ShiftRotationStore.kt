/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import ir.taqvim.core.model.Jdn
import ir.taqvim.core.workdays.ShiftRotation
import kotlinx.coroutines.flow.Flow

/**
 * The user's shift rotations (F-08), bound in `:app` over `shift_rotations` and `shift_rotation_records`.
 *
 * Both tables, their DAO and their backup mapping have existed since T-504 with nothing writing to them: the
 * calendar menu's "shift work" item opened a "not available yet" notice. This is the writer.
 */
interface ShiftRotationStore {
    /** Every rotation with the day exceptions it carries; re-emits on every change. */
    fun rotations(): Flow<List<ShiftRotation>>

    /** Saves [rotation], adding it when its id is 0; returns the id it was stored under. */
    suspend fun save(rotation: ShiftRotation): Long

    /** Deletes the rotation and, with it, its day exceptions. */
    suspend fun delete(id: Long)

    /** Sets the shift worked on [day] of [rotationId]; a `null` [shift] removes the exception. */
    suspend fun setException(
        rotationId: Long,
        day: Jdn,
        shift: String?,
    )
}
