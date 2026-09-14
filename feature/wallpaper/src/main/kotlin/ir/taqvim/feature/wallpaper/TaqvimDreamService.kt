/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.wallpaper

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext

/** The daydream's views: a large system clock with today's dates below it, dim grey on black. */
object DreamClockViews {
    const val TAG_CLOCK: String = "dream_clock"
    const val TAG_DATE: String = "dream_date"
    const val TAG_OTHER_DATES: String = "dream_other_dates"
    private const val CLOCK_SP = 72f
    private const val DATE_SP = 24f
    private const val OTHER_SP = 16f
    private const val DIM = 0xFFB0B0B0.toInt()

    /** A new daydream view; the clock follows the system's 12/24-hour setting and ticks by itself. */
    fun create(context: Context): View {
        val column =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                addView(TextClock(context).styled(TAG_CLOCK, CLOCK_SP))
                addView(TextView(context).styled(TAG_DATE, DATE_SP))
                addView(TextView(context).styled(TAG_OTHER_DATES, OTHER_SP))
            }
        return FrameLayout(context).apply {
            setBackgroundColor(Color.BLACK)
            addView(column, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
        }
    }

    /** Shows [content] under the clock of [root]; without content only the clock is shown. */
    fun bind(
        root: View,
        content: DreamContent?,
    ) {
        root.findViewWithTag<TextView>(TAG_DATE)?.show(content?.date.orEmpty())
        root.findViewWithTag<TextView>(TAG_OTHER_DATES)?.show(content?.otherDates.orEmpty())
    }

    private fun TextView.show(value: String) {
        text = value
        visibility = if (value.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun <T : TextView> T.styled(
        tag: String,
        sp: Float,
    ): T =
        apply {
            this.tag = tag
            setTextColor(DIM)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
            gravity = Gravity.CENTER
        }

    private const val WRAP = FrameLayout.LayoutParams.WRAP_CONTENT
}

/**
 * One daydream session (T-1215): shows [DreamClockViews] when attached, loads today's dates and reloads them only
 * when they change ([WallpaperRedrawPolicy]); detaching stops everything.
 */
class DreamSession(
    private val context: Context,
    private val source: DreamContentSource?,
    private val ticker: WallpaperTicker,
    private val clock: Clock = Clock.System,
) {
    private var scope: CoroutineScope? = null

    /** The shown view while attached. */
    var view: View? = null
        private set

    /** Creates the view, hands it to [show] and starts loading its dates. */
    fun attach(show: (View) -> Unit) {
        detach()
        val root = DreamClockViews.create(context)
        show(root)
        view = root
        scope = MainScope()
        load()
    }

    fun detach() {
        ticker.cancel()
        scope?.cancel()
        scope = null
        view = null
    }

    private fun load() {
        val current = scope ?: return
        current.launch {
            val now = clock.now()
            val content =
                source?.let {
                    withContext(Dispatchers.Default) { WallpaperRedrawPolicy.loadOrNull { it.load(now) } }
                }
            view?.let { DreamClockViews.bind(it, content) }
            ticker.schedule(WallpaperRedrawPolicy.delay(now, content?.nextChangeAt), ::load)
        }
    }
}

/**
 * Daydream / screen saver (T-1215): a clock with today's dates while charging or docked. Not interactive (a touch
 * wakes the device) and not bright, to save the screen and battery. Only the system can bind it (`BIND_DREAM_SERVICE`).
 */
class TaqvimDreamService : DreamService() {
    private var session: DreamSession? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = false
        val source = GlobalContext.getOrNull()?.getOrNull<DreamContentSource>()
        val started = DreamSession(this, source, HandlerWallpaperTicker(Handler(Looper.getMainLooper())))
        session = started
        started.attach { setContentView(it) }
    }

    override fun onDetachedFromWindow() {
        session?.detach()
        session = null
        super.onDetachedFromWindow()
    }
}
