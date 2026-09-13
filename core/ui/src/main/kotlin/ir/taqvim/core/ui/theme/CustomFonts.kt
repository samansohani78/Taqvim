/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import java.io.File

/** Font file formats a user may import. */
public enum class FontFileFormat {
    /** TrueType outlines (`.ttf`), sfnt version 0x00010000 or `true`. */
    TRUE_TYPE,

    /** OpenType with CFF outlines (`.otf`), sfnt version `OTTO`. */
    OPEN_TYPE_CFF,
}

/** Why an imported font file was not accepted. */
public enum class FontRejection {
    /** The file does not exist or cannot be read. */
    UNREADABLE,

    /** The file is not a single TrueType or OpenType font (collections are not supported). */
    UNSUPPORTED_FORMAT,

    /** The file announces a supported format but the platform cannot create a typeface from it. */
    INVALID_FONT,
}

/** Outcome of loading a user font file. */
public sealed interface CustomFontResult {
    /** The file is a supported font; [fontFamily] renders with it. */
    public data class Loaded(
        public val fontFamily: FontFamily,
        public val format: FontFileFormat,
    ) : CustomFontResult

    /** The file was not accepted. */
    public data class Rejected(
        public val reason: FontRejection,
    ) : CustomFontResult
}

/**
 * The custom-font hook of the theme (T-700): the settings screen copies an imported file into app storage and loads
 * it here; the resulting family is passed to [TaqvimTheme]. The sfnt version tags follow the OpenType specification
 * (Microsoft Typography, "The OpenType Font File", table directory).
 */
public object CustomFonts {
    private const val HEADER_BYTES = 4
    private val TRUE_TYPE_TAGS = listOf(byteArrayOf(0, 1, 0, 0), "true".toByteArray(Charsets.US_ASCII))
    private val CFF_TAG = "OTTO".toByteArray(Charsets.US_ASCII)

    /** The format announced by a font file's first bytes, or `null` when it is not a supported single font. */
    public fun detectFormat(header: ByteArray): FontFileFormat? {
        val tag = header.take(HEADER_BYTES)
        return when {
            tag.size < HEADER_BYTES -> null
            TRUE_TYPE_TAGS.any { it.toList() == tag } -> FontFileFormat.TRUE_TYPE
            CFF_TAG.toList() == tag -> FontFileFormat.OPEN_TYPE_CFF
            else -> null
        }
    }

    /** Loads [file] as a font family after checking its format. Reads only the first four bytes. */
    public fun load(file: File): CustomFontResult {
        val header =
            runCatching {
                file.inputStream().use { stream ->
                    val buffer = ByteArray(HEADER_BYTES)
                    buffer.copyOf(stream.read(buffer).coerceAtLeast(0))
                }
            }.getOrNull() ?: return CustomFontResult.Rejected(FontRejection.UNREADABLE)
        val format = detectFormat(header) ?: return CustomFontResult.Rejected(FontRejection.UNSUPPORTED_FORMAT)
        val family = runCatching { FontFamily(Font(file)) }.getOrNull()
        return family?.let { CustomFontResult.Loaded(it, format) }
            ?: CustomFontResult.Rejected(FontRejection.INVALID_FONT)
    }
}
