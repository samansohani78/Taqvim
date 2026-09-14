/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

/**
 * Unicode bidirectional isolates (UAX #9, Unicode 6.3+) for mixed-direction text (T-1701). A Persian or Arabic
 * paragraph reorders neutral characters around numbers, so "۲۵ + ۱" is shown as "۱ + ۲۵" and a Latin identifier in
 * parentheses can swap places with its neighbours. Isolating the embedded run keeps it in its own direction without
 * affecting the surrounding text.
 */
public object BidiText {
    /** LEFT-TO-RIGHT ISOLATE. */
    public const val LRI: Char = '⁦'

    /** RIGHT-TO-LEFT ISOLATE. */
    public const val RLI: Char = '⁧'

    /** FIRST STRONG ISOLATE: the run takes the direction of its first strong character. */
    public const val FSI: Char = '⁨'

    /** POP DIRECTIONAL ISOLATE: closes the most recent isolate. */
    public const val PDI: Char = '⁩'

    private val CONTROLS = setOf(LRI, RLI, FSI, PDI)

    /** [text] in a first-strong isolate: names, titles and other runs whose direction comes from their content. */
    public fun isolate(text: String): String = "$FSI$text$PDI"

    /** [text] forced left-to-right: arithmetic expressions, versions, coordinates, identifiers and URLs. */
    public fun ltr(text: String): String = "$LRI$text$PDI"

    /** [text] forced right-to-left. */
    public fun rtl(text: String): String = "$RLI$text$PDI"

    /** [text] without isolate controls, for parsing, search keys and clipboard text. */
    public fun strip(text: String): String = text.filterNot { it in CONTROLS }
}
