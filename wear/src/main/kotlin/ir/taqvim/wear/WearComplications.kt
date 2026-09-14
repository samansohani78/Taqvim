/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import android.content.Context
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

/** Date complication (T-1600): the day of the primary month, titled with the month name. */
class DateComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val today = wearGraph().today()
        return ComplicationContent.shortText(today.dayLabel, today.monthName, today.primaryDate)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        ComplicationContent.shortText(
            PREVIEW_DAY,
            getString(R.string.wear_complication_date),
            getString(R.string.wear_complication_date),
        )
}

/** Month progress complication (T-1600): how far the primary month has gone, with the day as text. */
class MonthProgressComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val today = wearGraph().today()
        return ComplicationContent.monthProgress(today, monthProgressDescription(this, today))
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        ComplicationContent.rangedValue(
            PREVIEW_VALUE,
            PREVIEW_MAX,
            PREVIEW_DAY,
            getString(R.string.wear_complication_month_progress),
        )
}

/** Next prayer complication (T-1600): the clock time of the next prayer at the chosen place; no data without one. */
class NextPrayerComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val next = wearGraph().today().nextPrayer ?: return NoDataComplicationData()
        val name = prayerName(next.prayer)
        return ComplicationContent.shortText(
            next.clock,
            name,
            getString(R.string.wear_next_prayer_at, name, next.clock),
        )
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        ComplicationContent.shortText(
            PREVIEW_CLOCK,
            getString(R.string.wear_prayer_dhuhr),
            getString(R.string.wear_complication_next_prayer),
        )
}

private const val PREVIEW_DAY = "12"
private const val PREVIEW_CLOCK = "12:05"
private const val PREVIEW_VALUE = 12f
private const val PREVIEW_MAX = 30f

/** "Day d of n" for the month progress complication, in the user's digits. */
internal fun monthProgressDescription(
    context: Context,
    today: WearToday,
): String = context.getString(R.string.wear_month_progress_description, today.dayLabel, today.monthName)

/** Complication data of the watch's complication services. */
object ComplicationContent {
    fun shortText(
        text: String,
        title: String,
        description: String,
    ): ShortTextComplicationData =
        ShortTextComplicationData
            .Builder(plain(text), plain(description))
            .setTitle(plain(title))
            .build()

    fun monthProgress(
        today: WearToday,
        description: String,
    ): RangedValueComplicationData =
        rangedValue(today.dayOfMonth.toFloat(), today.monthLength.toFloat(), today.dayLabel, description)

    fun rangedValue(
        value: Float,
        max: Float,
        text: String,
        description: String,
    ): RangedValueComplicationData =
        RangedValueComplicationData
            .Builder(value, 0f, max, plain(description))
            .setText(plain(text))
            .build()

    private fun plain(text: String) = PlainComplicationText.Builder(text).build()
}
