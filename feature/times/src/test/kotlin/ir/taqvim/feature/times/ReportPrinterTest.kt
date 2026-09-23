/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.times

import android.app.Activity
import android.content.Context
import android.webkit.WebView
import com.sun.management.HotSpotDiagnosticMXBean
import java.io.File
import java.lang.management.ManagementFactory
import java.lang.ref.WeakReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

/**
 * `WebView.createPrintDocumentAdapter` only works while the WebView it came from is alive, and nothing but
 * [ReportPrinter] holds a reference once `TimesRoute` fires `onPrintReport` and moves on. These tests drive the
 * object graph the way the framework does — creating the WebView through the same entry point production code uses
 * — then force a real collection ([forceFullCollection], the same heap-dump technique `MemoryLeakTest` uses)
 * instead of trusting that a reference merely *looks* reachable from the test's own source.
 *
 * Robolectric's WebView has no print engine, so once a page has "loaded" `createPrintDocumentAdapter` always fails
 * there; [ReportPrinter] treats that failure the same way it treats an unavailable print service, by releasing the
 * WebView. That is what lets these tests observe cleanup: it is the same `release` a successful `onFinish`, a
 * cancelled job and a failed one all end up calling, so exercising it here also covers those.
 */
@RunWith(RobolectricTestRunner::class)
class ReportPrinterTest {
    @Test
    fun `the WebView survives garbage collection while its page is still loading`() {
        val reference = printAndReferenceItsWebView(simulateSuccessfulLoad = false)

        repeat(GC_ATTEMPTS) { forceFullCollection() }

        assertNotNull("the WebView was collected before its page finished loading", reference.get())
    }

    @Test
    fun `the WebView is destroyed once its print job cannot go ahead`() {
        var webView: WebView? = null
        ReportPrinter.print(activity(), HTML, "job") { context -> newLoadedWebView(context) { webView = it } }

        ShadowLooper.idleMainLooper()

        assertTrue("the WebView was not destroyed once its print job ended", shadowOf(webView).wasDestroyCalled())
    }

    @Test
    fun `the WebView is collected once its print job ends`() {
        val reference = printAndReferenceItsWebView(simulateSuccessfulLoad = true)

        ShadowLooper.idleMainLooper()
        repeat(GC_ATTEMPTS) { forceFullCollection() }

        assertNull("the WebView was not released once its print job ended", reference.get())
    }

    @Test
    fun `printing twice in quick succession tracks each WebView on its own`() {
        val webViews = mutableListOf<WebView>()
        val newWebView: (Context) -> WebView = { context -> newLoadedWebView(context, webViews::add) }
        val activity = activity()

        ReportPrinter.print(activity, HTML, "job-1", newWebView)
        ReportPrinter.print(activity, HTML, "job-2", newWebView)
        ShadowLooper.idleMainLooper()

        assertEquals(2, webViews.size)
        webViews.forEach {
            assertTrue("every WebView must be released once its own job ends", shadowOf(it).wasDestroyCalled())
        }
    }

    /**
     * Runs [ReportPrinter.print] and returns only a [WeakReference] to the WebView it created, in its own stack
     * frame so no local variable of the calling test keeps it reachable while the test forces collection. When
     * [simulateSuccessfulLoad] is false the shadow is left "still loading" (Robolectric's default), matching the
     * window between `loadDataWithBaseURL` and `onPageFinished` that the original bug lost the WebView in.
     */
    private fun printAndReferenceItsWebView(simulateSuccessfulLoad: Boolean): WeakReference<WebView> {
        var reference: WeakReference<WebView>? = null
        ReportPrinter.print(activity(), HTML, "job") { context ->
            WebView(context).also { view ->
                if (simulateSuccessfulLoad) shadowOf(view).performSuccessfulPageLoadClientCallbacks()
                reference = WeakReference(view)
            }
        }
        return requireNotNull(reference)
    }

    /**
     * A WebView told to report a successful page load once [ReportPrinter] starts one, so idling the main looper
     * runs `onPageFinished` (`ShadowWebView` only queues that call once told the load succeeded).
     */
    private fun newLoadedWebView(
        context: Context,
        onCreated: (WebView) -> Unit,
    ): WebView =
        WebView(context).also { view ->
            shadowOf(view).performSuccessfulPageLoadClientCallbacks()
            onCreated(view)
        }

    private fun activity(): Activity = Robolectric.buildActivity(Activity::class.java).setup().get()

    /**
     * `System.gc()` is only a hint and does not clear a reference reliably; a live heap dump always runs a full
     * collection first (the same trick `MemoryLeakTest` uses). The dump itself is discarded.
     */
    private fun forceFullCollection() {
        val dump = File.createTempFile("taqvim-report-printer-", ".hprof")
        dump.delete()
        ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean::class.java).dumpHeap(dump.path, true)
        dump.delete()
    }

    private companion object {
        const val GC_ATTEMPTS = 3
        const val HTML = "<html><body>Report</body></html>"
    }
}
