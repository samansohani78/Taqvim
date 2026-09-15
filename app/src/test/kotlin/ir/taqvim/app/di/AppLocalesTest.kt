/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import android.app.Application
import android.app.LocaleManager
import android.os.LocaleList
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.app.R
import ir.taqvim.core.i18n.LanguageTable
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.xmlpull.v1.XmlPullParser

/** T-1501 (R): the per-app language on Android 13+, the stored language before it, and the system locale list. */
@RunWith(AndroidJUnit4::class)
class AppLocalesTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    // Robolectric starts TaqvimApplication, and so Koin, for every test.
    @After
    fun stopDependencyGraph() {
        stopKoin()
    }

    @Test
    fun theLocaleManagerKeepsTheAppLanguage() {
        val manager = application.getSystemService(LocaleManager::class.java)
        manager.applicationLocales = LocaleList.getEmptyLocaleList()
        val locales = platformAppLocales(application)
        assertTrue(locales is LocaleManagerAppLocales)
        assertNull(locales.current())

        locales.apply("fa-AF")

        assertEquals("fa-AF", locales.current())
        assertEquals("fa-AF", manager.applicationLocales.toLanguageTags())
    }

    @Test
    fun theLegacyLanguageIsStoredAnnouncedAndAppliedToContexts() {
        val locales = LegacyAppLocales(application)
        assertNull(locales.current())
        val announced =
            runBlocking {
                val first =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        withTimeout(TIMEOUT_MILLIS) { locales.applied.first() }
                    }
                locales.apply("ckb-IQ")
                first.await()
            }

        assertEquals("ckb-IQ", announced)
        assertEquals("ckb-IQ", LegacyAppLocales(application).current())
        val wrapped = LegacyAppLocales.wrap(application, "ckb-IQ").resources.configuration
        assertEquals("ckb", wrapped.locales[0].language)
        assertEquals(View.LAYOUT_DIRECTION_RTL, wrapped.layoutDirection)
        // Android 13+ keeps the language itself, so activity contexts are not wrapped there.
        assertSame(application, LegacyAppLocales.wrap(application))
    }

    @Test
    fun theSystemOffersEveryLaunchLanguage() {
        val parser = application.resources.getXml(R.xml.locales_config)
        val tags = mutableListOf<String>()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                tags += parser.getAttributeValue(ANDROID_NAMESPACE, "name")
            }
        }

        assertEquals(LanguageTable.languages.map { it.localeTag }, tags)
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
