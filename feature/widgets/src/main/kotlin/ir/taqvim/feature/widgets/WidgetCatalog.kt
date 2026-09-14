/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget

/** Launcher receiver of [DateWidget1x1] (T-1201). */
class DateWidget1x1Receiver : TaqvimWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DateWidget1x1()
}

/** Launcher receiver of [DateClockWidget4x1] (T-1202). */
class DateClockWidget4x1Receiver : TaqvimWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DateClockWidget4x1()
}

/** Launcher receiver of [DaySummaryWidget2x2] (T-1203). */
class DaySummaryWidget2x2Receiver : TaqvimWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DaySummaryWidget2x2()
}

/** Launcher receiver of [PrayerStripWidget4x2] (T-1204). */
class PrayerStripWidget4x2Receiver : TaqvimWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerStripWidget4x2()
}

/** Launcher receiver of [MonthInteractiveWidget] (T-1205). */
class MonthInteractiveWidgetReceiver : TaqvimWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthInteractiveWidget()
}

/** Launcher receiver of [MonthBitmapWidget] (T-1206). */
class MonthBitmapWidgetReceiver : TaqvimWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthBitmapWidget()
}

/** Launcher receiver of [WeekStripWidget] (T-1207). */
class WeekStripWidgetReceiver : TaqvimWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekStripWidget()
}

/** Launcher receiver of [ScheduleWidget] (T-1208). */
class ScheduleWidgetReceiver : TaqvimWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()
}

/** Launcher receiver of [SunArcWidget] (T-1209). */
class SunArcWidgetReceiver : TaqvimWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SunArcWidget()
}

/** Launcher receiver of [MoonWidget] (T-1210). */
class MoonWidgetReceiver : TaqvimWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MoonWidget()
}

/** Launcher receiver of [MapWidget] (T-1211). */
class MapWidgetReceiver : TaqvimWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MapWidget()
}

/** Launcher receiver of [CountdownWidget] (T-1212). */
class CountdownWidgetReceiver : TaqvimWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownWidget()
}

/** The widgets implemented so far, registered with the framework by `widgetsFeatureModule`. */
object WidgetCatalog {
    val registrations: List<WidgetRegistration> =
        listOf(
            WidgetRegistration(
                WidgetKind.DATE_1X1,
                DateWidget1x1Receiver::class.java,
                DateWidget1x1::class.java,
            ) { DateWidget1x1() },
            WidgetRegistration(
                WidgetKind.DATE_CLOCK_4X1,
                DateClockWidget4x1Receiver::class.java,
                DateClockWidget4x1::class.java,
            ) { DateClockWidget4x1() },
            WidgetRegistration(
                WidgetKind.DAY_SUMMARY_2X2,
                DaySummaryWidget2x2Receiver::class.java,
                DaySummaryWidget2x2::class.java,
            ) { DaySummaryWidget2x2() },
            WidgetRegistration(
                WidgetKind.PRAYER_STRIP_4X2,
                PrayerStripWidget4x2Receiver::class.java,
                PrayerStripWidget4x2::class.java,
            ) { PrayerStripWidget4x2() },
            WidgetRegistration(
                WidgetKind.MONTH_INTERACTIVE,
                MonthInteractiveWidgetReceiver::class.java,
                MonthInteractiveWidget::class.java,
            ) { MonthInteractiveWidget() },
            WidgetRegistration(
                WidgetKind.MONTH_BITMAP,
                MonthBitmapWidgetReceiver::class.java,
                MonthBitmapWidget::class.java,
            ) { MonthBitmapWidget() },
            WidgetRegistration(
                WidgetKind.WEEK_STRIP,
                WeekStripWidgetReceiver::class.java,
                WeekStripWidget::class.java,
            ) { WeekStripWidget() },
            WidgetRegistration(
                WidgetKind.SCHEDULE,
                ScheduleWidgetReceiver::class.java,
                ScheduleWidget::class.java,
            ) { ScheduleWidget() },
            WidgetRegistration(
                WidgetKind.SUN_ARC,
                SunArcWidgetReceiver::class.java,
                SunArcWidget::class.java,
            ) { SunArcWidget() },
            WidgetRegistration(
                WidgetKind.MOON,
                MoonWidgetReceiver::class.java,
                MoonWidget::class.java,
            ) { MoonWidget() },
            WidgetRegistration(WidgetKind.MAP, MapWidgetReceiver::class.java, MapWidget::class.java) { MapWidget() },
            WidgetRegistration(
                WidgetKind.COUNTDOWN,
                CountdownWidgetReceiver::class.java,
                CountdownWidget::class.java,
            ) { CountdownWidget() },
        )
}

/** Prayer names from the widget string resources. */
class ResourceWidgetPrayerNames(
    private val context: Context,
) : WidgetPrayerNames {
    override fun name(prayer: WidgetPrayer): String =
        context.getString(
            when (prayer) {
                WidgetPrayer.FAJR -> R.string.widget_prayer_fajr
                WidgetPrayer.SUNRISE -> R.string.widget_prayer_sunrise
                WidgetPrayer.DHUHR -> R.string.widget_prayer_dhuhr
                WidgetPrayer.ASR -> R.string.widget_prayer_asr
                WidgetPrayer.MAGHRIB -> R.string.widget_prayer_maghrib
                WidgetPrayer.ISHA -> R.string.widget_prayer_isha
            },
        )
}
