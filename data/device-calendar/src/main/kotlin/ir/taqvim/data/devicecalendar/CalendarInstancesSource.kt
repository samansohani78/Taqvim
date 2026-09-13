/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.devicecalendar

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.provider.CalendarContract
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/** Outcome of reading instances from the device calendar provider. */
sealed interface InstancesResult {
    /** Every instance row overlapping the window, unfiltered. */
    data class Rows(
        val rows: List<InstanceRow>,
    ) : InstancesResult

    /** `READ_CALENDAR` is not granted (or was revoked during the query); nothing was read. */
    data object PermissionDenied : InstancesResult

    /** The provider is missing or failed; any cached instances are still valid. */
    data object Unavailable : InstancesResult
}

/** Read access to device calendar instances and their change notifications. */
interface InstancesSource {
    fun hasPermission(): Boolean

    /** Instances overlapping [window]. */
    fun query(window: InstantWindow): InstancesResult

    /** Emits whenever the calendar provider reports a change; conflated. */
    fun changes(): Flow<Unit>
}

/**
 * [InstancesSource] over `CalendarContract.Instances` (T-602). Queries are permission-aware and never throw; changes
 * come from a [ContentObserver] on the whole calendar authority.
 */
class CalendarInstancesSource(
    private val context: Context,
) : InstancesSource {
    private val resolver: ContentResolver
        get() = context.contentResolver

    override fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    override fun query(window: InstantWindow): InstancesResult {
        if (!hasPermission()) return InstancesResult.PermissionDenied
        val uri =
            CalendarContract.Instances.CONTENT_URI
                .buildUpon()
                .also { ContentUris.appendId(it, window.fromEpochMillis) }
                .also { ContentUris.appendId(it, window.toEpochMillis) }
                .build()
        return runCatching { resolver.query(uri, PROJECTION.toTypedArray(), null, null, SORT_ORDER)?.use(::readRows) }
            .fold(
                onSuccess = { rows -> rows?.let(InstancesResult::Rows) ?: InstancesResult.Unavailable },
                onFailure = { error ->
                    if (error is SecurityException) InstancesResult.PermissionDenied else InstancesResult.Unavailable
                },
            )
    }

    override fun changes(): Flow<Unit> =
        callbackFlow {
            val observer =
                object : ContentObserver(null) {
                    override fun onChange(selfChange: Boolean) {
                        trySend(Unit)
                    }
                }
            val registered =
                runCatching { resolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer) }
                    .isSuccess
            awaitClose { if (registered) resolver.unregisterContentObserver(observer) }
        }.conflate()

    private fun readRows(cursor: Cursor): List<InstanceRow> {
        val index = PROJECTION.associateWith(cursor::getColumnIndexOrThrow)

        fun long(column: String) = cursor.getLong(index.getValue(column))

        fun flag(column: String) = cursor.getInt(index.getValue(column)) == 1

        fun nullableInt(column: String) =
            index.getValue(column).let { if (cursor.isNull(it)) null else cursor.getInt(it) }

        return buildList {
            while (cursor.moveToNext()) {
                add(
                    InstanceRow(
                        eventId = long(CalendarContract.Instances.EVENT_ID),
                        calendarId = long(CalendarContract.Instances.CALENDAR_ID),
                        title = cursor.getString(index.getValue(CalendarContract.Instances.TITLE)),
                        beginEpochMillis = long(CalendarContract.Instances.BEGIN),
                        endEpochMillis = long(CalendarContract.Instances.END),
                        allDay = flag(CalendarContract.Instances.ALL_DAY),
                        displayColor = nullableInt(CalendarContract.Instances.DISPLAY_COLOR),
                        visible = flag(CalendarContract.Instances.VISIBLE),
                        deleted = flag(CalendarContract.Events.DELETED),
                    ),
                )
            }
        }
    }

    internal companion object {
        /** Columns read from `CalendarContract.Instances`. */
        val PROJECTION: List<String> =
            listOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.CALENDAR_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.DISPLAY_COLOR,
                CalendarContract.Instances.VISIBLE,
                CalendarContract.Events.DELETED,
            )

        private const val SORT_ORDER = "${CalendarContract.Instances.BEGIN} ASC"
    }
}
