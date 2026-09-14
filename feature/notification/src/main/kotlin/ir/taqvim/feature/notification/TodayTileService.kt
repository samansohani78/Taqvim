/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import android.annotation.SuppressLint
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext

/** What the Quick Settings tile shows: today's date, the weekday and the day number of its icon. */
data class TodayTileState(
    val label: String,
    val subtitle: String,
    val description: String,
    val iconText: String,
)

/** Builds [TodayTileState] from a [TodaySummary]. */
object TodayTileStates {
    /** The tile of [summary]; [describe] joins the weekday and the date for accessibility. */
    fun of(
        summary: TodaySummary,
        describe: (weekday: String, date: String) -> String,
    ): TodayTileState =
        TodayTileState(
            label = summary.title,
            subtitle = summary.weekday,
            description = describe(summary.weekday, summary.title),
            iconText = summary.dayNumber,
        )
}

/**
 * Quick Settings tile (T-1215) showing today's date with the day number as its icon; a tap opens today in the app,
 * after unlocking when the device is locked. The tile has no on/off state, so it is always shown as active. Only the
 * system can bind it (`BIND_QUICK_SETTINGS_TILE`).
 */
class TodayTileService : TileService() {
    private val scope = MainScope()

    override fun onStartListening() {
        super.onStartListening()
        val koin = GlobalContext.getOrNull() ?: return
        val source = koin.getOrNull<TodaySummarySource>() ?: return
        val icons = koin.getOrNull<DayIconCache>() ?: return
        scope.launch {
            val summary =
                withContext(Dispatchers.Default) { TodaySurfaces.attempt { source.load(Clock.System.now()) } }
            if (summary != null) show(TodayTileStates.of(summary, ::describe), icons)
        }
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun(::openToday) else openToday()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun describe(
        weekday: String,
        date: String,
    ): String = getString(R.string.notification_tile_description, weekday, date)

    private fun show(
        state: TodayTileState,
        icons: DayIconCache,
    ) {
        val tile = qsTile ?: return
        val sizePx = (TILE_ICON_DP * resources.displayMetrics.density).toInt()
        tile.label = state.label
        tile.contentDescription = state.description
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = state.subtitle
        tile.icon = Icon.createWithBitmap(icons.icon(state.iconText, sizePx, DayIconStyle.GLYPH))
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openToday() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(TodaySurfaces.openTodayPendingIntent(this, REQUEST_CODE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(TodaySurfaces.openToday(this))
        }
    }

    private companion object {
        const val TILE_ICON_DP = 24f
        const val REQUEST_CODE = 1_215
    }
}
