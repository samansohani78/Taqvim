/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import ir.taqvim.core.workdays.WorkdayProfile
import kotlinx.coroutines.flow.Flow

/** A stored workday profile (F-07): its [id], the [name] the user gave it, the [profile] and whether it is default. */
data class NamedWorkdayProfile(
    val id: Long,
    val name: String,
    val profile: WorkdayProfile,
    val isDefault: Boolean,
)

/**
 * The user's workday profiles (F-07), bound in `:app` over `workday_profiles`.
 *
 * The table and its DAO have existed since T-504; nothing wrote to them, so every user ran on the fallback weekend.
 * This is the port the editor writes through.
 */
interface WorkdayProfileStore {
    /** Every profile, the default first; re-emits on every change. */
    fun profiles(): Flow<List<NamedWorkdayProfile>>

    /** Saves [profile] under [name], adding it when [id] is `null`; returns the id it was stored under. */
    suspend fun save(
        id: Long?,
        name: String,
        profile: WorkdayProfile,
    ): Long

    /** Deletes the profile [id]; deleting the default leaves no default until one is chosen. */
    suspend fun delete(id: Long)

    /** Makes [id] the only default profile, which is the one the workday calculator uses. */
    suspend fun makeDefault(id: Long)
}
