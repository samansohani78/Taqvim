/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.devicecalendar

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CalendarContract
import org.robolectric.Robolectric

/**
 * Synthetic `com.android.calendar` provider registered through Robolectric's content resolver shadow. It serves
 * [rows] as `CalendarContract.Instances` columns and ignores selection, so filtering is left to the adapter.
 */
class FakeCalendarProvider : ContentProvider() {
    val rows: MutableList<InstanceRow> = mutableListOf()
    val queried: MutableList<Uri> = mutableListOf()
    var failure: RuntimeException? = null
    var answerNull: Boolean = false

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        queried += uri
        failure?.let { throw it }
        val columns = projection ?: CalendarInstancesSource.PROJECTION.toTypedArray()
        return if (answerNull) null else MatrixCursor(columns).apply { rows.forEach { addRow(values(columns, it)) } }
    }

    private fun values(
        columns: Array<out String>,
        row: InstanceRow,
    ): List<Any?> {
        val byColumn =
            mapOf(
                CalendarContract.Instances.EVENT_ID to row.eventId,
                CalendarContract.Instances.CALENDAR_ID to row.calendarId,
                CalendarContract.Instances.TITLE to row.title,
                CalendarContract.Instances.BEGIN to row.beginEpochMillis,
                CalendarContract.Instances.END to row.endEpochMillis,
                CalendarContract.Instances.ALL_DAY to flag(row.allDay),
                CalendarContract.Instances.DISPLAY_COLOR to row.displayColor,
                CalendarContract.Instances.VISIBLE to flag(row.visible),
                CalendarContract.Events.DELETED to flag(row.deleted),
            )
        return columns.map { byColumn[it] }
    }

    private fun flag(value: Boolean): Int = if (value) 1 else 0

    override fun getType(uri: Uri): String? = null

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? = null

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        /** Creates the provider and registers it for the calendar authority in the current Robolectric test. */
        fun install(): FakeCalendarProvider =
            Robolectric
                .buildContentProvider(FakeCalendarProvider::class.java)
                .create(CalendarContract.AUTHORITY)
                .get()
    }
}
