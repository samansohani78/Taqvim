/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sun.management.HotSpotDiagnosticMXBean
import ir.taqvim.data.scheduler.AlarmInputWatcher
import ir.taqvim.data.scheduler.PreferenceChangeWatcher
import java.io.File
import java.lang.management.ManagementFactory
import java.lang.ref.WeakReference
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowLooper

/**
 * T-1803: nothing that lives as long as the process keeps a screen alive. The Koin graph and the watchers started by
 * [TaqvimApplication] (preferences, alarm inputs, widget triggers) run for the whole process, so an Activity reachable
 * from them would leak with every configuration change.
 */
@RunWith(AndroidJUnit4::class)
class MemoryLeakTest {
    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun mainActivityIsCollectedAfterItIsDestroyed() {
        val reference = launchAndDestroy()

        assertTrue("MainActivity is still reachable after onDestroy", isCollected(reference))
    }

    /**
     * Runs a full activity lifecycle in its own frame, so no local variable of the test keeps the activity or its
     * controller alive while the test waits for collection.
     */
    private fun launchAndDestroy(): WeakReference<MainActivity> {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val reference = WeakReference(controller.get())
        controller.pause().stop().destroy()
        return reference
    }

    @Test
    fun processLifetimeObjectsUseTheApplicationContext() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            val koin = GlobalContext.get()

            assertTrue("Koin's androidContext must be the Application", koin.get<Context>() is Application)
            assertNotNull(koin.get<PreferenceChangeWatcher>())
            assertNotNull(koin.get<AlarmInputWatcher>())
        }
    }

    /** Whether [reference] is cleared after up to [GC_ATTEMPTS] full collections, running pending main-looper work. */
    private fun isCollected(reference: WeakReference<*>): Boolean {
        repeat(GC_ATTEMPTS) {
            if (reference.get() == null) return true
            ShadowLooper.idleMainLooper()
            forceFullCollection()
        }
        return reference.get() == null
    }

    /**
     * `System.gc()` is only a hint and did not clear the reference reliably; a live heap dump always runs a full
     * collection first. The dump itself is discarded.
     */
    private fun forceFullCollection() {
        val dump = File.createTempFile("taqvim-leak-", ".hprof")
        dump.delete()
        ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean::class.java).dumpHeap(dump.path, true)
        dump.delete()
    }

    private companion object {
        const val GC_ATTEMPTS = 3
    }
}
