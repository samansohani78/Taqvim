/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.theme

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

/** Font creation needs the platform typeface loader, so the accepted and invalid paths run on Robolectric. */
@RunWith(AndroidJUnit4::class)
class CustomFontsRobolectricTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun file(
        name: String,
        bytes: ByteArray,
    ): File = folder.newFile(name).apply { writeBytes(bytes) }

    @Test
    fun loadsSupportedFile() {
        val result = CustomFonts.load(file("synthetic.otf", "OTTO".toByteArray() + ByteArray(8)))
        assertTrue(result is CustomFontResult.Loaded && result.format == FontFileFormat.OPEN_TYPE_CFF)
    }
}
