/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear.device

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.connection.DefaultTileClient
import androidx.wear.tiles.renderer.TileRenderer
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.wear.MonthTileService
import ir.taqvim.wear.NextTileService
import ir.taqvim.wear.WearMonthBuilder
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T-1600 on a real watch (Gradle Managed Device `wearApi34`): both tile services are bound as the system binds them,
 * answer a tile request, and their layouts are rendered by the ProtoLayout renderer. The rendered text is checked
 * against what the watch graph computes, and each rendered tile is stored as a PNG in the test output.
 */
@RunWith(AndroidJUnit4::class)
class WearTileDeviceTest {
    private val executor = Executors.newSingleThreadExecutor()

    @Before
    fun setUp() {
        assumeWatch()
        useLanguage("fa")
    }

    @Test
    fun theMonthTileRendersTheCurrentMonth() {
        val month =
            runBlocking {
                val setup = graph.setup()
                WearMonthBuilder.build(
                    setup,
                    graph.calculator.lookup(setup.islamicVariant),
                    graph.clock.now().toJdn(setup.zone),
                )
            }
        renderTile(MonthTileService::class.java, "wear_tile_month") { texts ->
            check(texts.firstOrNull() == month.title) {
                "the month tile shows ${texts.firstOrNull()}, expected ${month.title}"
            }
            val days =
                month.weeks
                    .flatten()
                    .filterNotNull()
                    .map { it.label }
            check(texts.containsAll(days)) { "the month tile is missing days of $days: $texts" }
        }
    }

    @Test
    fun theNextTileRendersTodayAndTheNextPrayer() {
        val today = runBlocking { graph.today() }
        renderTile(NextTileService::class.java, "wear_tile_next") { texts ->
            check(texts.firstOrNull() == today.primaryDate) {
                "the next tile shows ${texts.firstOrNull()}, expected ${today.primaryDate}"
            }
            check(texts.size >= MINIMUM_NEXT_LINES) { "the next tile shows only $texts" }
        }
    }

    /** Requests the tile of [service], renders it, checks its texts with [assert] and stores it as `<name>.png`. */
    private fun renderTile(
        service: Class<*>,
        name: String,
        assert: (List<String>) -> Unit,
    ) {
        val tile = requestTile(service)
        val layout =
            requireNotNull(
                tile.tileTimeline
                    ?.timelineEntries
                    ?.firstOrNull()
                    ?.layout,
            ) { "$name has no layout" }
        val view = inflate(layout)
        assert(texts(view))
        store(view, name)
    }

    /** Binds [service] as the system does and asks it for a tile. */
    private fun requestTile(service: Class<*>): TileBuilders.Tile {
        val client = DefaultTileClient(appContext, ComponentName(appContext, service), executor)
        val parameters =
            DeviceParametersBuilders.DeviceParameters
                .Builder()
                .setScreenWidthDp(
                    device.displayWidth /
                        appContext.resources.displayMetrics.density
                            .toInt(),
                ).setScreenHeightDp(
                    device.displayHeight /
                        appContext.resources.displayMetrics.density
                            .toInt(),
                ).setScreenDensity(appContext.resources.displayMetrics.density)
                .setScreenShape(DeviceParametersBuilders.SCREEN_SHAPE_ROUND)
                .setDevicePlatform(DeviceParametersBuilders.DEVICE_PLATFORM_WEAR_OS)
                .build()
        val request =
            RequestBuilders.TileRequest
                .Builder()
                .setDeviceConfiguration(parameters)
                .build()
        return client.requestTile(request).get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    /** Inflates [layout] with the ProtoLayout renderer into a screen-sized parent, on the main thread. */
    private fun inflate(layout: LayoutElementBuilders.Layout): View {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val resources =
            ResourceBuilders.Resources
                .Builder()
                .setVersion(RESOURCES_VERSION)
                .build()
        val parent = FrameLayout(appContext)
        var inflated: View? = null
        instrumentation.runOnMainSync {
            val renderer = TileRenderer(appContext, executor) { }
            inflated = renderer.inflateAsync(layout, resources, parent).get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        }
        val view = requireNotNull(inflated) { "the renderer returned no view" }
        instrumentation.runOnMainSync {
            val width = View.MeasureSpec.makeMeasureSpec(device.displayWidth, View.MeasureSpec.EXACTLY)
            val height = View.MeasureSpec.makeMeasureSpec(device.displayHeight, View.MeasureSpec.EXACTLY)
            parent.measure(width, height)
            parent.layout(0, 0, device.displayWidth, device.displayHeight)
        }
        return parent
    }

    /** Every text of the rendered view, in layout order. */
    private fun texts(view: View): List<String> =
        when (view) {
            is TextView -> listOf(view.text.toString())
            is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
            else -> emptyList()
        }

    /** Draws [view] into `<name>.png` in the managed device's test output directory. */
    private fun store(
        view: View,
        name: String,
    ) {
        val bitmap = Bitmap.createBitmap(device.displayWidth, device.displayHeight, Bitmap.Config.ARGB_8888)
        InstrumentationRegistry.getInstrumentation().runOnMainSync { view.draw(Canvas(bitmap)) }
        val file = File(outputDirectory(), "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
        check(file.length() > 0) { "$file was not written" }
    }

    /** Where the managed device collects files (`additionalTestOutputDir`), or the app's own files as a fallback. */
    private fun outputDirectory(): File {
        val argument = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val directory = argument?.let(::File) ?: File(appContext.getExternalFilesDir(null), "screenshots")
        directory.mkdirs()
        return directory
    }

    private companion object {
        const val REQUEST_TIMEOUT_SECONDS = 30L
        const val RESOURCES_VERSION = "1"
        const val PNG_QUALITY = 100

        /** The next tile shows the date and at least the next prayer line. */
        const val MINIMUM_NEXT_LINES = 2
    }
}
