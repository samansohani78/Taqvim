/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** The modules of a QR code, row by row, without the quiet zone; `true` is dark. */
data class QrMatrix(
    val size: Int,
    val cells: ImmutableList<Boolean>,
) {
    init {
        require(cells.size == size * size) { "a $size × $size matrix needs ${size * size} cells (was ${cells.size})" }
    }

    fun isDark(
        x: Int,
        y: Int,
    ): Boolean = cells[y * size + x]
}

/** QR encoding (ISO/IEC 18004) through ZXing core (Apache-2.0): UTF-8 byte mode, error correction level M. */
internal object QrEncoder {
    /** Longest accepted text in characters; ZXing fails anyway on data beyond the largest version. */
    const val MAX_LENGTH: Int = 1_000

    fun encode(text: String): QrState =
        when {
            text.isEmpty() -> {
                QrState.Empty
            }

            text.length > MAX_LENGTH -> {
                QrState.TooLong
            }

            else -> {
                runCatching { Encoder.encode(text, ErrorCorrectionLevel.M, HINTS).matrix }
                    .map { matrix ->
                        val size = matrix.width
                        QrMatrix(
                            size,
                            (0 until size * size).map { matrix.get(it % size, it / size) == DARK }.toImmutableList(),
                        )
                    }.fold(onSuccess = { QrState.Code(it) }, onFailure = { QrState.TooLong })
            }
        }

    private const val DARK: Byte = 1
    private val HINTS = mapOf(EncodeHintType.CHARACTER_SET to Charsets.UTF_8.name())
}
