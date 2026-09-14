/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import android.content.Context
import androidx.concurrent.futures.SuspendToFutureAdapter
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.TypeBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import ir.taqvim.core.calendar.toJdn
import kotlinx.coroutines.Dispatchers

/** A tile whose single layout is computed from the watch graph on a background dispatcher. */
abstract class TaqvimTileService : TileService() {
    /** The tile's layout now. */
    protected abstract suspend fun layout(graph: WearGraph): LayoutElement

    /** How long the tile stays valid before the system asks again. */
    protected open val freshnessMillis: Long = TileLayouts.MINUTE_MILLIS

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        SuspendToFutureAdapter.launchFuture(Dispatchers.Default) { currentTile() }

    /** The tile now; what [onTileRequest] answers. */
    internal suspend fun currentTile(): TileBuilders.Tile = TileLayouts.tile(layout(wearGraph()), freshnessMillis)

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> =
        SuspendToFutureAdapter.launchFuture(Dispatchers.Default) {
            ResourceBuilders.Resources
                .Builder()
                .setVersion(TileLayouts.RESOURCES_VERSION)
                .build()
        }
}

/** Month tile (T-1600): today's month of the primary calendar with today and holidays marked. */
class MonthTileService : TaqvimTileService() {
    override val freshnessMillis: Long = TileLayouts.HOUR_MILLIS

    override suspend fun layout(graph: WearGraph): LayoutElement {
        val setup = graph.setup()
        val today = graph.clock.now().toJdn(setup.zone)
        return TileLayouts.month(WearMonthBuilder.build(setup, graph.calculator.lookup(setup.islamicVariant), today))
    }
}

/** Next tile (T-1600): the next prayer at the chosen place and the next official occasion. */
class NextTileService : TaqvimTileService() {
    override suspend fun layout(graph: WearGraph): LayoutElement {
        val today = graph.today()
        return TileLayouts.next(today.primaryDate, nextLines(this, today))
    }
}

/** The lines of the next tile: next prayer (or how to get one), then the next occasion when there is one. */
internal fun nextLines(
    context: Context,
    today: WearToday,
): List<String> =
    listOfNotNull(
        today.nextPrayer?.let {
            context.getString(
                R.string.wear_next_prayer_at,
                context.prayerName(it.prayer),
                it.clock,
            )
        }
            ?: context.getString(R.string.wear_no_place),
        today.nextEvent?.let { event ->
            context.getString(R.string.wear_event_line, event.title, context.daysAway(event))
        },
    )

/** Tile layouts built with ProtoLayout; plain builders so tests can read them back with [texts]. */
object TileLayouts {
    const val RESOURCES_VERSION: String = "1"
    const val MINUTE_MILLIS: Long = 60_000L
    const val HOUR_MILLIS: Long = 60 * MINUTE_MILLIS

    private const val TITLE_SP = 15f
    private const val BODY_SP = 13f
    private const val CELL_SP = 11f
    private const val CELL_DP = 22f
    private const val CELL_HEIGHT_DP = 18f
    private const val ON_SURFACE = 0xFFFFFFFF.toInt()
    private const val DIM = 0xFFB0B0B0.toInt()
    private const val ACCENT = 0xFF8AB4F8.toInt()
    private const val HOLIDAY = 0xFFF28B82.toInt()

    fun tile(
        layout: LayoutElement,
        freshnessMillis: Long,
    ): TileBuilders.Tile =
        TileBuilders.Tile
            .Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(freshnessMillis)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout))
            .build()

    /** A title and body lines, centered. */
    fun next(
        title: String,
        lines: List<String>,
    ): LayoutElement = column(listOf(text(title, TITLE_SP, ACCENT)) + lines.map { text(it, BODY_SP, ON_SURFACE) })

    /** The month title, weekday initials and one row per week. */
    fun month(month: WearMonth): LayoutElement {
        val header = row(month.weekdayLabels.map { cell(it, DIM) })
        val weeks = month.weeks.map { week -> row(week.map { cell(it?.label.orEmpty(), colorOf(it)) }) }
        return column(listOf(text(month.title, BODY_SP, ACCENT), header) + weeks)
    }

    /** Every text of [element] in layout order. */
    fun texts(element: LayoutElement?): List<String> =
        when (element) {
            is LayoutElementBuilders.Text -> listOfNotNull(element.text?.value)
            is LayoutElementBuilders.Column -> element.contents.flatMap(::texts)
            is LayoutElementBuilders.Row -> element.contents.flatMap(::texts)
            is LayoutElementBuilders.Box -> element.contents.flatMap(::texts)
            else -> emptyList()
        }

    private fun colorOf(cell: WearMonthCell?): Int =
        when {
            cell == null -> DIM
            cell.isToday -> ACCENT
            cell.isHoliday || cell.isWeekend -> HOLIDAY
            else -> ON_SURFACE
        }

    private fun text(
        value: String,
        sizeSp: Float,
        color: Int,
    ): LayoutElement =
        LayoutElementBuilders.Text
            .Builder()
            .setText(TypeBuilders.StringProp.Builder(value).build())
            .setMaxLines(2)
            .setFontStyle(
                LayoutElementBuilders.FontStyle
                    .Builder()
                    .setSize(sp(sizeSp))
                    .setColor(argb(color))
                    .build(),
            ).build()

    private fun cell(
        value: String,
        color: Int,
    ): LayoutElement =
        LayoutElementBuilders.Box
            .Builder()
            .setWidth(dp(CELL_DP))
            .setHeight(dp(CELL_HEIGHT_DP))
            .addContent(text(value, CELL_SP, color))
            .build()

    private fun row(items: List<LayoutElement>): LayoutElement =
        items.fold(LayoutElementBuilders.Row.Builder()) { builder, item -> builder.addContent(item) }.build()

    private fun column(items: List<LayoutElement>): LayoutElement =
        items
            .fold(
                LayoutElementBuilders.Column
                    .Builder()
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER),
            ) { builder, item -> builder.addContent(item) }
            .build()
}
