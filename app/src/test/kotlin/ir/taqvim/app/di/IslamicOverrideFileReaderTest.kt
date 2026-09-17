/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** ADR-0037: override files are read through the content resolver, small ones whole and large ones refused. */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class IslamicOverrideFileReaderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val reader = ContentOverrideFileReader(context)

    private fun file(
        name: String,
        bytes: ByteArray,
    ): String = Uri.fromFile(File(context.cacheDir, name).apply { writeBytes(bytes) }).toString()

    @Test
    fun smallFilesAreReadWhole(): Unit =
        runTest {
            assertEquals(OverrideFileText.Read("{}"), reader.read(file("small.json", "{}".toByteArray())))
        }

    @Test
    fun largeFilesAreRefused(): Unit =
        runTest {
            assertEquals(OverrideFileText.TooLarge, reader.read(file("large.json", ByteArray(OVERRIDE_MAX_BYTES + 1))))
        }

    @Test
    fun missingFilesAreUnreadable(): Unit =
        runTest {
            assertEquals(OverrideFileText.Unreadable, reader.read("file:///no/such/file.json"))
            assertEquals(OverrideFileText.Unreadable, reader.read("content://ir.taqvim.nowhere/none"))
        }
}
