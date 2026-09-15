/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.benchmark.micro

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ir.taqvim.core.ui.painter.BitmapPainter
import ir.taqvim.core.ui.painter.PainterEnvironment
import ir.taqvim.feature.map.Illumination
import ir.taqvim.feature.map.LayerGrids
import ir.taqvim.feature.map.SubPoints
import ir.taqvim.feature.widgets.WidgetDrawings
import ir.taqvim.feature.widgets.WidgetMap
import ir.taqvim.feature.widgets.WidgetPainters
import ir.taqvim.feature.widgets.WidgetSkyBuilder
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T-1801 widget render timings (plan §9: any widget render < 30 ms, budgets in `benchmark/budgets.json`): each T-702
 * painter draws a new bitmap at the size of its widget, as the Glance widgets do on every update. Glance composition
 * itself runs in the launcher's RemoteViews pipeline and is not measured here (ADR-0018 addendum).
 */
@RunWith(AndroidJUnit4::class)
class WidgetRenderBenchmark {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val density = context.resources.displayMetrics.density
    private val painters = WidgetPainters(BenchmarkModels.PALETTE, PainterEnvironment(density, rtl = true))

    @Test
    fun monthBitmap4x4(): Unit = render("monthBitmap4x4", painters.month, BenchmarkModels.month(), LARGE, TITLE_DP)

    @Test
    fun sunArcBitmap(): Unit = render("sunArcBitmap", painters.sunArc, BenchmarkModels.sun(), WIDE, TITLE_DP)

    @Test
    fun moonBitmap(): Unit = render("moonBitmap", painters.moon, BenchmarkModels.moon(), SMALL, TITLE_DP)

    @Test
    fun progressRingBitmap(): Unit = render("progressRingBitmap", painters.ring, BenchmarkModels.ring(), SMALL, 0f)

    /** The map thumbnail with the bundled world outline and a widget-sized night shade already computed. */
    @Test
    fun mapThumbnailBitmap(): Unit =
        render("mapThumbnailBitmap", painters.map, WidgetDrawings.mapModel(map()), WIDE, 0f)

    /** The whole map widget update: the twilight grid, its darkness values, the model and the bitmap. */
    @Test
    fun mapWidgetContent() {
        val (width, height) = WidgetDrawings.pixels(WIDE, density, 0f)
        val timing = Timing.measure { painters.map.paint(WidgetDrawings.mapModel(map()), width, height).recycle() }
        TimingReport.record(CLASS_NAME, "mapWidgetContent", timing)
    }

    private val land by lazy { BenchmarkModels.outline(context).land }

    /** The map widget's content as `WidgetSkyParts` in `:app` builds it. */
    private fun map(): WidgetMap {
        val grid = LayerGrids.illumination(SubPoints.sun(BenchmarkModels.EQUINOX), WIDGET_COLUMNS, WIDGET_ROWS)
        val darkest = (Illumination.entries.size - 1).toFloat()
        return WidgetSkyBuilder.map(
            land,
            grid.columns,
            grid.rows,
            { column, row -> grid[column, row] / darkest },
            BenchmarkModels.MARKER,
        )
    }

    private fun <M> render(
        name: String,
        painter: BitmapPainter<M>,
        model: M,
        size: DpSize,
        reservedHeightDp: Float,
    ) {
        val (width, height) = WidgetDrawings.pixels(size, density, reservedHeightDp)
        TimingReport.record(CLASS_NAME, name, Timing.measure { painter.paint(model, width, height).recycle() })
    }

    private companion object {
        val CLASS_NAME: String = WidgetRenderBenchmark::class.java.name

        /** The largest launcher sizes of 4 × 4, 4 × 2 and 2 × 2 cells (phone portrait, in dp). */
        val LARGE = DpSize(320.dp, 400.dp)
        val WIDE = DpSize(320.dp, 180.dp)
        val SMALL = DpSize(160.dp, 180.dp)

        /** Room for the widget's title line. */
        const val TITLE_DP = 22f

        /** The map widget's twilight grid (`WidgetSkyParts` in `:app`). */
        const val WIDGET_COLUMNS = 72
        const val WIDGET_ROWS = 36
    }
}
