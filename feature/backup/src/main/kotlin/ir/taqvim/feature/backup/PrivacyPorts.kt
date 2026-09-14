/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.backup

import kotlinx.coroutines.flow.Flow

/** Kinds of data Taqvim keeps on the device (F-15). */
enum class StoredDataKind {
    PERSONAL_EVENTS,
    REMINDERS,
    SUBSCRIPTIONS,
    LOCATION,
    DEVICE_CALENDAR_CACHE,
    DIAGNOSTICS,
    RECENT_SEARCHES,
    ;

    /** Whether the privacy dashboard can clear this data; personal data is managed in its own screens. */
    val clearable: Boolean
        get() = this in CLEARABLE

    private companion object {
        val CLEARABLE = setOf(LOCATION, DEVICE_CALENDAR_CACHE, DIAGNOSTICS, RECENT_SEARCHES)
    }
}

/** How much of one kind of data is stored: the number of entries, or 1 for a chosen location. */
data class StoredData(
    val kind: StoredDataKind,
    val count: Int,
)

/** Permissions Taqvim may use; every feature works without them until it is used. */
enum class PermissionKind {
    LOCATION,
    CALENDAR,
    NOTIFICATIONS,
    EXACT_ALARMS,
    DO_NOT_DISTURB,
}

data class PermissionStatus(
    val kind: PermissionKind,
    val granted: Boolean,
)

/** What data is stored and how to clear it; bound in `:app` over the database, preferences and caches. */
interface PrivacyDataSource {
    fun storedData(): Flow<List<StoredData>>

    /** Clears the data of [kind]; returns whether it was cleared. Only [StoredDataKind.clearable] kinds are asked. */
    suspend fun clear(kind: StoredDataKind): Boolean
}

/** Which permissions are granted; bound in `:app` over the platform permission checks. */
interface PermissionStatusSource {
    fun statuses(): Flow<List<PermissionStatus>>

    /** Checks the permissions again, e.g. after the user returns from the system settings. */
    fun refresh()
}
