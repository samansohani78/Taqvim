/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.theme

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CustomFontsTest {
    @TempDir
    lateinit var directory: Path

    private fun file(
        name: String,
        bytes: ByteArray,
    ): File = directory.resolve(name).toFile().apply { writeBytes(bytes) }

    @Test
    fun `sfnt version tags identify supported formats`() {
        CustomFonts.detectFormat(byteArrayOf(0, 1, 0, 0, 0, 12)) shouldBe FontFileFormat.TRUE_TYPE
        CustomFonts.detectFormat("true".toByteArray()) shouldBe FontFileFormat.TRUE_TYPE
        CustomFonts.detectFormat("OTTO".toByteArray()) shouldBe FontFileFormat.OPEN_TYPE_CFF
        CustomFonts.detectFormat("ttcf".toByteArray()).shouldBeNull()
        CustomFonts.detectFormat("wOFF".toByteArray()).shouldBeNull()
        CustomFonts.detectFormat(byteArrayOf(0, 1)).shouldBeNull()
        CustomFonts.detectFormat(byteArrayOf()).shouldBeNull()
    }

    @Test
    fun `loading checks the file before creating a font family`() {
        CustomFonts.load(file("notes.txt", "hello".toByteArray())) shouldBe
            CustomFontResult.Rejected(FontRejection.UNSUPPORTED_FORMAT)
        CustomFonts.load(file("empty.ttf", byteArrayOf())) shouldBe
            CustomFontResult.Rejected(FontRejection.UNSUPPORTED_FORMAT)
        CustomFonts.load(directory.resolve("missing.ttf").toFile()) shouldBe
            CustomFontResult.Rejected(FontRejection.UNREADABLE)
        // Plain JVM host tests have no platform typeface loader, so creating the family fails like a corrupt font.
        CustomFonts.load(file("corrupt.ttf", byteArrayOf(0, 1, 0, 0, 9, 9))) shouldBe
            CustomFontResult.Rejected(FontRejection.INVALID_FONT)
    }
}
