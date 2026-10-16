/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import ir.taqvim.core.i18n.BidiText
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Every widget, inflated as real `RemoteViews` at every span it offers, must keep its text inside its box.
 *
 * `ThreeDatesRenderingTest` holds T-1216 to this and found the type that was written for the wrong bucket. Nothing
 * held the other twelve to it, and a Glance semantics tree cannot: it says a `Text` exists, not where the inflated
 * `TextView` ended up. The device suite cannot either — it reads the host's view tree for *some* readable text, and
 * a widget whose second line falls off the bottom still has a first line.
 *
 * The widgets draw real values here, because an empty string clips nothing and would make this pass for free.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class EveryWidgetFitsItsBoxTest {
    @Test
    fun everyWidgetKeepsItsTextInsideEverySpanItOffers() {
        val clipped = mutableListOf<String>()
        WidgetCatalog.registrations.forEach { registration ->
            registration.kind.sizes.forEach { size ->
                clipped += clippedLabels(registration, size)
            }
        }
        check(clipped.isEmpty()) {
            "These widgets draw text outside the box their own span gives them:\n" + clipped.joinToString("\n")
        }
    }

    private fun clippedLabels(
        registration: WidgetRegistration,
        size: WidgetSize,
    ): List<String> {
        val context = Robolectric.buildActivity(Activity::class.java).setup().get()
        val config = WidgetConfig.defaultFor(registration.kind)
        val widget = registration.create() as TaqvimGlanceWidget
        val remoteViews =
            runBlocking {
                GlanceRemoteViews()
                    .compose(context, DpSize(size.widthDp.dp, size.heightDp.dp)) {
                        GlanceTheme {
                            WidgetFrame(config, onClick = null) { style ->
                                widget.Content(WidgetSamples.data(), config, style, size)
                            }
                        }
                    }.remoteViews
            }
        val parent = FrameLayout(context)
        context.setContentView(parent)
        parent.addView(remoteViews.apply(context, parent))
        parent.measure(exactly(size.widthDp), exactly(size.heightDp))
        parent.layout(0, 0, size.widthDp, size.heightDp)
        parent.draw(Canvas(Bitmap.createBitmap(size.widthDp, size.heightDp, Bitmap.Config.ARGB_8888)))

        return texts(parent)
            .filter { it.text.isNotBlank() }
            .mapNotNull { label ->
                val bounds = Rect()
                label.getDrawingRect(bounds)
                parent.offsetDescendantRectToMyCoords(label, bounds)
                val outside = bounds.top < 0 || bounds.bottom > size.heightDp
                val invisible = bounds.width() == 0 || bounds.height() == 0
                val why =
                    when {
                        outside -> "outside the ${size.heightDp} dp box"
                        invisible -> "laid out ${bounds.width()}x${bounds.height()}"
                        else -> return@mapNotNull null
                    }
                "  ${registration.kind}/$size: '${BidiText.strip(label.text.toString())}' $why ($bounds)"
            }
    }

    private fun exactly(size: Int) = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)

    private fun texts(view: View): List<TextView> =
        when (view) {
            is TextView -> listOf(view)
            is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
            else -> emptyList()
        }
}
