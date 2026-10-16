/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.device

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RemoteViews
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry

/**
 * What a launcher actually ended up showing for one widget.
 *
 * The point of this type is the distinction the old test missed. A host shows a widget's
 * `android:initialLayout` — for every Taqvim widget, `@layout/glance_default_loading_layout`, a bare
 * `ProgressBar` — from the moment it is bound until Glance delivers its first `RemoteViews`. "The host received
 * something" and "the user can read their date" are therefore different facts, and only the second one is the
 * product working.
 */
internal data class RenderedWidget(
    val provider: String,
    /** How many non-null `RemoteViews` the host applied. Zero means Glance never composed at all. */
    val updates: Int,
    val showedErrorView: Boolean,
    /** Every non-blank text in the applied view tree. */
    val texts: List<String>,
    /** Images that actually carry a drawable, which is how the drawn widgets (month, Moon, sun, map) render. */
    val drawnImages: Int,
    val progressBars: Int,
    /**
     * Milliseconds from the update broadcast until the host first showed readable content, or `null` if it never
     * did.
     *
     * This is the only latency figure in the project measured where the user experiences it: through a real
     * [AppWidgetHost], from the trigger to content on screen. `GlanceWidgetBenchmark` measures composition inside
     * the test process and says so — it cannot see the host inflate anything. Two honest limits apply here: the
     * resolution is [WidgetHostProbe.POLL_RESOLUTION_MILLIS] because the wait polls, so this is an upper bound
     * rounded up to that interval; and it counts whatever a `whileUpdating` hook did, so it only means latency
     * when that hook is the default.
     */
    val millisToContent: Long? = null,
) {
    /**
     * Whether the host is still showing the loading placeholder: nothing was composed, or what was composed
     * carries no readable content at all.
     */
    val isLoadingPlaceholder: Boolean
        get() = updates == 0 || (texts.isEmpty() && drawnImages == 0)

    override fun toString(): String =
        "$provider(updates=$updates, texts=$texts, drawnImages=$drawnImages, " +
            "progressBars=$progressBars, errorView=$showedErrorView, millisToContent=$millisToContent)"
}

/**
 * Fails unless [this] is the finished widget a user would see.
 *
 * A loading placeholder fails here by construction (T-1200). Every assertion names the provider and the whole
 * captured state, because "widget did not render" without saying what it did render costs an hour on a device.
 */
internal fun RenderedWidget.assertRenderedFinalContent() {
    check(updates > 0) {
        "$provider never delivered RemoteViews: the host is still showing glance_default_loading_layout. $this"
    }
    check(!showedErrorView) { "$provider showed the launcher's error view. $this" }
    check(!isLoadingPlaceholder) {
        "$provider delivered RemoteViews with no readable content, so the user still sees a spinner. $this"
    }
}

/** Fails unless the widget drew at least [count] separate non-blank texts (e.g. the three dates of T-1216). */
internal fun RenderedWidget.assertShowsAtLeastTexts(count: Int) {
    assertRenderedFinalContent()
    check(texts.size >= count) { "$provider drew ${texts.size} texts, expected at least $count. $this" }
}

/** Fails unless some drawn text contains [substring] — proof of a specific value, not merely of some text. */
internal fun RenderedWidget.assertSomeTextContains(substring: String) {
    assertRenderedFinalContent()
    check(texts.any { it.contains(substring) }) { "$provider drew no text containing '$substring'. $this" }
}

/**
 * Binds every installed widget provider through a real [AppWidgetHost], asks each to update, and reports what the
 * host ended up showing.
 *
 * [onEach] is called per provider so a caller can wait for its own condition; the default waits for the first
 * delivery. The host is a real one, so this exercises the same path a launcher does.
 */
internal class WidgetHostProbe(
    private val context: Context,
    hostId: Int,
) {
    private val host = RecordingHost(context, hostId)

    fun start() {
        onMain { host.startListening() }
    }

    fun stop() {
        onMain { host.stopListening() }
        host.deleteHost()
    }

    fun providers(): List<AppWidgetProviderInfo> =
        AppWidgetManager
            .getInstance(context)
            .getInstalledProvidersForPackage(context.packageName, null)

    /**
     * Binds [info] in [activityContext], triggers an update and waits up to [timeoutMillis] for real content.
     *
     * [whileUpdating] runs after the update broadcast is sent and before the wait begins, which is the window in
     * which Glance's `AsyncRequestWorker` is enqueued but has not drawn yet. A caller that wants to prove
     * something survives an upgrade puts it there, because that is exactly where the v1.3.0 cancel landed.
     */
    fun render(
        info: AppWidgetProviderInfo,
        activityContext: Context,
        timeoutMillis: Long = DEVICE_TIMEOUT_MILLIS,
        whileUpdating: () -> Unit = {},
    ): RenderedWidget {
        val manager = AppWidgetManager.getInstance(context)
        val id = host.allocateAppWidgetId()
        var startedAt = SystemClock.uptimeMillis()
        try {
            check(manager.bindAppWidgetIdIfAllowed(id, info.provider)) { "Cannot bind ${info.provider}" }
            val view = checkNotNull(onMain { host.createView(activityContext, id, info) as? RecordingHostView })
            startedAt = SystemClock.uptimeMillis()
            context.sendBroadcast(
                Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .setComponent(info.provider)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(id)),
            )
            whileUpdating()
            // Waiting for readable content, not for a delivery: the first delivery may still be a placeholder.
            val rendered =
                runCatching {
                    waitFor("${info.provider.shortClassName} to draw its content", timeoutMillis) {
                        !capture(info, view).isLoadingPlaceholder
                    }
                }.isSuccess
            val elapsed = SystemClock.uptimeMillis() - startedAt
            return capture(info, view).copy(millisToContent = elapsed.takeIf { rendered })
        } finally {
            host.deleteAppWidgetId(id)
        }
    }

    private fun capture(
        info: AppWidgetProviderInfo,
        view: RecordingHostView,
    ): RenderedWidget {
        val texts = mutableListOf<String>()
        var drawn = 0
        var progress = 0
        onMain {
            view.walk { child ->
                when (child) {
                    is TextView -> {
                        val text = child.text?.toString().orEmpty()
                        if (text.isNotBlank()) texts += text
                    }

                    is ImageView -> {
                        if (child.drawable != null) drawn++
                    }

                    is ProgressBar -> {
                        progress++
                    }
                }
            }
        }
        return RenderedWidget(
            provider = info.provider.shortClassName,
            updates = view.updates,
            showedErrorView = view.failed,
            texts = texts,
            drawnImages = drawn,
            progressBars = progress,
        )
    }

    private fun <T> onMain(block: () -> T): T {
        var result: Result<T>? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result = runCatching(block) }
        return checkNotNull(result).getOrThrow()
    }

    companion object {
        /**
         * The granularity of [RenderedWidget.millisToContent]: `waitFor` polls, so a figure is the first poll at
         * or after the content appeared. Kept in step with `POLL_MILLIS` in `DeviceApp.kt` by hand, because a
         * measurement that silently claims more precision than it has is worse than no measurement.
         */
        const val POLL_RESOLUTION_MILLIS: Long = 200
    }

    /** A host whose views record their updates and whether the launcher's error view was shown. */
    private class RecordingHost(
        context: Context,
        hostId: Int,
    ) : AppWidgetHost(context, hostId) {
        override fun onCreateView(
            context: Context,
            appWidgetId: Int,
            appWidget: AppWidgetProviderInfo?,
        ): AppWidgetHostView = RecordingHostView(context)
    }

    private class RecordingHostView(
        context: Context,
    ) : AppWidgetHostView(context) {
        @Volatile
        var updates: Int = 0

        @Volatile
        var failed: Boolean = false

        override fun updateAppWidget(remoteViews: RemoteViews?) {
            super.updateAppWidget(remoteViews)
            if (remoteViews != null) updates++
        }

        override fun getErrorView(): View {
            failed = true
            return super.getErrorView()
        }
    }
}

/** Applies [visit] to every view in the tree rooted at this one. */
private fun View.walk(visit: (View) -> Unit) {
    visit(this)
    if (this is ViewGroup) {
        for (index in 0 until childCount) childAt(index)?.walk(visit)
    }
}

private fun ViewGroup.childAt(index: Int): View? = runCatching { getChildAt(index) }.getOrNull()
