/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.feature.settings.AthanSoundChoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Robolectric

/** T-1101 platform glue: picked sounds keep their name, and an unreadable preview reports that it did not start. */
@RunWith(AndroidJUnit4::class)
class AthanPlatformTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    // Robolectric starts TaqvimApplication, and so Koin, for every test.
    @After
    fun stopDependencyGraph() {
        stopKoin()
    }

    @Test
    fun adoptedSoundsCarryTheirDisplayName(): Unit =
        runTest {
            Robolectric.setupContentProvider(SoundsProvider::class.java, AUTHORITY)
            val library = ContentResolverAthanSoundLibrary(context.contentResolver, Dispatchers.Unconfined)
            val uri = "content://$AUTHORITY/sounds/1"

            assertEquals(AthanSoundChoice(uri, "Adhan Makkah"), library.adopt(uri))
        }

    @Test
    fun anUnreadableSoundDoesNotStartThePreview() {
        val preview = MediaPlayerAthanPreview(context)

        assertFalse(preview.play(AthanSoundChoice("content://$AUTHORITY/missing", null), 50) {})
        preview.stop()
        preview.stop()
    }

    /** Serves one sound's display name. */
    class SoundsProvider : ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)).apply { addRow(arrayOf("Adhan Makkah")) }

        override fun getType(uri: Uri): String = "audio/mpeg"

        override fun insert(
            uri: Uri,
            values: ContentValues?,
        ): Uri? = null

        override fun delete(
            uri: Uri,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0
    }

    private companion object {
        const val AUTHORITY = "ir.taqvim.test.sounds"
    }
}
