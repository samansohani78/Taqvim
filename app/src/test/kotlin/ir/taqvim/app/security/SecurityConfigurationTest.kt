/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.security

import android.app.AlarmManager
import android.app.Application
import android.content.Intent
import android.os.StrictMode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.app.DebugStrictMode
import ir.taqvim.app.automation.AutomationBroadcasts
import ir.taqvim.app.automation.DayChangeReceiver
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.w3c.dom.Element

/**
 * T-1804 (ADR-0017): HTTPS only with system CAs, Auto Backup only when end-to-end encrypted, StrictMode in debug, and
 * receivers ignore broadcasts they do not handle.
 */
@RunWith(AndroidJUnit4::class)
class SecurityConfigurationTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    // Robolectric starts TaqvimApplication, and so Koin, for every test.
    @After
    fun tearDown() {
        stopKoin()
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX)
        StrictMode.setVmPolicy(StrictMode.VmPolicy.LAX)
    }

    @Test
    fun cleartextTrafficIsRefusedAndOnlySystemAuthoritiesAreTrusted() {
        val config = xml("src/main/res/xml/network_security_config.xml")
        val base = config.getElementsByTagName("base-config").item(0) as Element

        assertEquals("false", base.getAttribute("cleartextTrafficPermitted"))
        val sources = elements(config, "certificates").map { it.getAttribute("src") }
        assertEquals(listOf("system"), sources)
        assertEquals(0, config.getElementsByTagName("domain-config").length)
        assertEquals(0, config.getElementsByTagName("debug-overrides").length)
    }

    @Test
    fun autoBackupRequiresEndToEndEncryption() {
        val legacy = xml("src/main/res/xml/backup_rules.xml")
        assertEquals(0, legacy.getElementsByTagName("include").length)
        assertEquals(DOMAINS, elements(legacy, "exclude").map { it.getAttribute("domain") }.toSet())

        val pie = elements(xml("src/main/res/xml-v28/backup_rules.xml"), "include")
        assertTrue(pie.isNotEmpty())
        assertTrue(pie.all { it.getAttribute("requireFlags") == "clientSideEncryption" })

        val extraction = xml("src/main/res/xml/data_extraction_rules.xml")
        val cloud = extraction.getElementsByTagName("cloud-backup").item(0) as Element
        assertEquals("true", cloud.getAttribute("disableIfNoEncryptionCapabilities"))
    }

    @Test
    fun strictModeIsOnlyInstalledInDebugBuilds() {
        // TaqvimApplication already installed the debug policies when Robolectric started it.
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX)
        StrictMode.setVmPolicy(StrictMode.VmPolicy.LAX)
        DebugStrictMode.install(debug = false)
        assertEquals(StrictMode.VmPolicy.LAX.toString(), StrictMode.getVmPolicy().toString())

        DebugStrictMode.install(debug = true)
        assertNotEquals(StrictMode.ThreadPolicy.LAX.toString(), StrictMode.getThreadPolicy().toString())
    }

    @Test
    fun theDayChangeReceiverIgnoresBroadcastsItDoesNotHandle() {
        DayChangeReceiver().onReceive(application, Intent("com.example.SPOOFED").putExtra("jdn", 1L))
        DayChangeReceiver().onReceive(application, Intent())

        assertTrue(shadowOf(application).broadcastIntents.none { it.action == AutomationBroadcasts.ACTION_DAY_CHANGED })
        assertTrue(shadowOf(application.getSystemService(AlarmManager::class.java)).scheduledAlarms.isEmpty())
    }

    private fun xml(path: String) =
        DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(File(path))

    private fun elements(
        document: org.w3c.dom.Document,
        tag: String,
    ): List<Element> {
        val nodes = document.getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private companion object {
        val DOMAINS = setOf("root", "file", "database", "sharedpref", "external")
    }
}
