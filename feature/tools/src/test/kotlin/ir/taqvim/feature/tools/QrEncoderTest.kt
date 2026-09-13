/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

/** T-1400 QR codes: what is encoded decodes back (ZXing's reader as an independent check), and the limits. */
class QrEncoderTest {
    /** Decodes [matrix] rendered with a quiet zone, four pixels per module. */
    private fun decode(matrix: QrMatrix): String {
        val scale = 4
        val quiet = 4
        val side = (matrix.size + 2 * quiet) * scale
        val pixels =
            IntArray(side * side) { index ->
                val x = index % side / scale - quiet
                val y = index / side / scale - quiet
                val dark = x in 0 until matrix.size && y in 0 until matrix.size && matrix.isDark(x, y)
                if (dark) BLACK else WHITE
            }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(side, side, pixels)))
        return QRCodeReader().decode(bitmap, mapOf(DecodeHintType.CHARACTER_SET to "UTF-8")).text
    }

    @Test
    fun `texts and links decode back`() {
        listOf("https://example.com/taqvim?day=1405-06-22", "تقویم ۱۴۰۵ — Taqvim", "a").forEach { text ->
            val code = QrEncoder.encode(text).shouldBeInstanceOf<QrState.Code>()
            (code.matrix.size in 21..177 && code.matrix.size % 4 == 1) shouldBe true
            decode(code.matrix) shouldBe text
        }
    }

    @Test
    fun `empty and oversized texts have no code`() {
        QrEncoder.encode("") shouldBe QrState.Empty
        QrEncoder.encode("x".repeat(QrEncoder.MAX_LENGTH + 1)) shouldBe QrState.TooLong
        // 1 000 three-byte characters exceed the largest version at level M.
        QrEncoder.encode("€".repeat(QrEncoder.MAX_LENGTH)) shouldBe QrState.TooLong
    }

    @Test
    fun `a matrix needs size squared cells`() {
        shouldThrow<IllegalArgumentException> { QrMatrix(2, persistentListOf(true, false, true)) }
        QrMatrix(1, persistentListOf(true)).isDark(0, 0) shouldBe true
    }

    private companion object {
        const val BLACK = 0xFF000000.toInt()
        const val WHITE = 0xFFFFFFFF.toInt()
    }
}
