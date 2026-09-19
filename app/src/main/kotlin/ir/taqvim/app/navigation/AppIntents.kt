/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.navigation

import android.content.Intent
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.nlp.AnchorLookup
import ir.taqvim.core.nlp.ParseContext
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

/** The screen an intent that starts or reaches the app asks for (T-1103): `taqvim://` links and selected text. */
internal object AppIntents {
    private const val FALLBACK_LANGUAGE = "en"

    /**
     * The destination of [intent], or `null` for intents that ask for no particular screen (such as the launcher's).
     * Selected text is read with [parseContext], created only when needed.
     */
    fun destination(
        intent: Intent?,
        parseContext: () -> ParseContext,
    ): AppDestination? =
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                intent.data
                    ?.takeIf { it.scheme.equals(DeepLinks.SCHEME, ignoreCase = true) }
                    ?.let { DeepLinks.parse(it.toString()) }
            }

            Intent.ACTION_PROCESS_TEXT -> {
                intent
                    .getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
                    ?.let { ProcessText.destination(it.toString(), parseContext()) }
            }

            else -> {
                null
            }
        }

    /**
     * How selected text is read: as in [languageCode] (English when unsupported), relative to the day of [now], with
     * [anchors] naming events for phrases such as "3 days before Nowruz" (review R02).
     */
    fun parseContext(
        now: Instant,
        zone: TimeZone,
        languageCode: String,
        anchors: AnchorLookup? = null,
    ): ParseContext {
        val today = now.toJdn(zone)
        val language = LanguageTable.forCode(languageCode) ?: LanguageTable.forCode(FALLBACK_LANGUAGE)
        return language?.let { ParseContext.forLanguage(it, today, anchors = anchors) }
            ?: ParseContext(today, anchors = anchors)
    }
}
