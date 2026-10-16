/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import ir.taqvim.core.i18n.BidiText
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Base of Taqvim's Glance widgets (T-1200). Each session loads the widget's stored configuration and shared content
 * through [WidgetStateLoader] (off the main thread, nothing cached across process death), lays out responsively for
 * the kind's [WidgetSize] buckets, and draws the configured background, transparency and scale around [Content].
 * Tapping the widget opens today in the app; the loading failure state is drawn by the framework.
 */
abstract class TaqvimGlanceWidget(
    val kind: WidgetKind,
) : GlanceAppWidget(),
    KoinComponent {
    override val sizeMode: SizeMode =
        SizeMode.Responsive(kind.sizes.map { DpSize(it.widthDp.dp, it.heightDp.dp) }.toSet())

    private val loader: WidgetStateLoader by inject()

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val state = loader.load(kind, GlanceAppWidgetManager(context).getAppWidgetId(id))
        val openToday = WidgetLinks.intent(context, WidgetClickTarget.Today)
        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                val bucket = WidgetSize.forAvailable(size.width.value, size.height.value, kind.sizes)
                WidgetFrame(state.config, openToday) { style ->
                    when (state) {
                        is WidgetContentState.Ready -> {
                            Content(state.data, state.config, style, bucket)
                        }

                        is WidgetContentState.Failed -> {
                            WidgetMessage(
                                context.getString(R.string.widget_load_failed),
                                style,
                            )
                        }
                    }
                }
            }
        }
    }

    /** The widget's own content for [size], drawn with [style] inside the configured frame. */
    @Composable
    @GlanceComposable
    abstract fun Content(
        data: WidgetData,
        config: WidgetConfig,
        style: WidgetStyle,
        size: WidgetSize,
    )
}

/** Text colors and scale of a widget's configuration, for its content. */
data class WidgetStyle(
    val foreground: ColorProvider,
    val holiday: ColorProvider,
    val scalePercent: Int,
) {
    /** A text style of [sizeSp] (before scaling), in the holiday color when [holiday]. */
    fun text(
        sizeSp: Float,
        holiday: Boolean = false,
        bold: Boolean = false,
    ): TextStyle =
        TextStyle(
            color = if (holiday) this.holiday else foreground,
            fontSize = WidgetAppearance.scaled(sizeSp, scalePercent).sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        )
}

/** The configured frame: background role with transparency, rounded corners and the whole-widget [onClick]. */
@Composable
@GlanceComposable
fun WidgetFrame(
    config: WidgetConfig,
    onClick: Intent?,
    content:
        @Composable @GlanceComposable
        (WidgetStyle) -> Unit,
) {
    val context = LocalContext.current
    val (background, foreground) = palette(config.background)
    val argb = WidgetAppearance.withTransparency(background.getColor(context).toArgb(), config.transparencyPercent)
    val framed = GlanceModifier.fillMaxSize().background(Color(argb)).cornerRadius(CORNER_RADIUS_DP.dp)
    Box(
        modifier = (onClick?.let { framed.clickable(actionStartActivity(it)) } ?: framed).padding(PADDING_DP.dp),
        contentAlignment = Alignment.Center,
    ) {
        content(WidgetStyle(foreground, GlanceTheme.colors.error, config.scalePercent))
    }
}

/**
 * The date lines most widgets start with: weekday, the day (number or full title) and the secondary date.
 *
 * Each date is directionally isolated, the way [ThreeDatesWidget] isolates its three. A date carries its own
 * reading order and the widget's comes from the app language, so the two disagree whenever a user keeps a calendar
 * written in the other direction — a Gregorian secondary date on a Persian widget, or a Persian primary one on an
 * English widget. Without an isolate the line is laid out in the widget's base direction and the trailing year or
 * a neutral separator moves to the wrong end.
 */
@Composable
@GlanceComposable
fun WidgetDateLines(
    data: WidgetData,
    config: WidgetConfig,
    style: WidgetStyle,
    compact: Boolean,
    size: WidgetSize,
    modifier: GlanceModifier = GlanceModifier,
) {
    val holiday = config.shows(WidgetContent.HOLIDAYS) && data.isHoliday
    val room = hasRoomForOptionalLines(size)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (room && config.shows(WidgetContent.WEEKDAY)) {
            Text(data.weekday, style = style.text(SMALL_SP, holiday), maxLines = 1)
        }
        Text(
            // The day number alone is a number: isolating it would only add controls the renderer must strip.
            text = if (compact) data.dayNumber else BidiText.isolate(data.title),
            style = style.text(if (compact) NUMBER_SP else TITLE_SP, holiday, bold = true),
            // A second line where there is one to spare. "10 October 2026" at 16 sp needs more than the 94 dp a
            // two-cell widget leaves after padding, and a date truncated to "10 Octob…" is worse than a date on
            // two lines — it is the one value the widget exists to show. One cell tall still gets a single line,
            // because there is no second line to give it.
            maxLines = if (room) TITLE_MAX_LINES else 1,
        )
        val secondary = data.secondaryDate
        if (room && config.shows(WidgetContent.SECONDARY_DATE) && secondary != null) {
            Text(BidiText.isolate(secondary), style = style.text(SMALL_SP), maxLines = 1)
        }
    }
}

/**
 * Whether [size] is tall enough for the weekday and the secondary date as well as the day itself.
 *
 * One cell is [WidgetSize.cellsToDp] 40 dp, and the frame takes [PADDING_DP] from each end of it. What is left
 * cannot hold the day line at all, let alone two more at [SMALL_SP] — so a one-cell-tall widget was laying its
 * lower lines out past its own bottom edge and the user saw the first line and a sliver of the second
 * (`EveryWidgetFitsItsBoxTest`, 2026-10-17). The day is what both of these widgets are for, so the day is what
 * stays; this is the same choice T-1216 makes when two cells cannot hold three dates.
 */
private fun hasRoomForOptionalLines(size: WidgetSize): Boolean = size.heightDp >= TWO_LINE_HEIGHT_DP

/** A short message in place of content (e.g. the loading failure). */
@Composable
@GlanceComposable
fun WidgetMessage(
    text: String,
    style: WidgetStyle,
) {
    Text(text, style = style.text(SMALL_SP), maxLines = MESSAGE_LINES)
}

@Composable
@GlanceComposable
private fun palette(background: WidgetBackground): Pair<ColorProvider, ColorProvider> =
    when (background) {
        WidgetBackground.SURFACE -> {
            GlanceTheme.colors.widgetBackground to GlanceTheme.colors.onSurface
        }

        WidgetBackground.PRIMARY_CONTAINER -> {
            GlanceTheme.colors.primaryContainer to
                GlanceTheme.colors.onPrimaryContainer
        }

        WidgetBackground.SECONDARY_CONTAINER -> {
            GlanceTheme.colors.secondaryContainer to GlanceTheme.colors.onSecondaryContainer
        }

        WidgetBackground.BLACK -> {
            ColorProvider(Color.Black) to ColorProvider(Color.White)
        }

        WidgetBackground.WHITE -> {
            ColorProvider(Color.White) to ColorProvider(Color.Black)
        }
    }

private const val CORNER_RADIUS_DP = 16
private const val PADDING_DP = 8

/**
 * The shortest box that holds the day plus the weekday and the secondary date.
 *
 * Two cells (110 dp) comfortably do; one cell (40 dp) cannot. The threshold sits between them rather than being
 * fitted to text metrics, because the question a widget can actually answer is how many cells tall it is.
 */
private const val TWO_LINE_HEIGHT_DP = 110

/** The day may take a second line wherever the box has one; see [WidgetDateLines]. */
private const val TITLE_MAX_LINES = 2
private const val SMALL_SP = 12f
private const val TITLE_SP = 16f
private const val NUMBER_SP = 28f
private const val MESSAGE_LINES = 3
