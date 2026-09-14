/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.compose.runtime.Immutable
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.i18n.PersianText
import java.util.Locale
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet

/** State of the settings home (T-1500). */
data class SettingsHomeUiState(
    val loading: Boolean = true,
    val tab: SettingsTab = SettingsTab.INTERFACE_CALENDAR,
    /** Text typed in the settings search; blank shows the rows of [tab]. */
    val query: String = "",
    /** Every item in catalog order; the screen shows those of [tab] or the search results. */
    val rows: ImmutableList<SettingsRow> = persistentListOf(),
    /** The item opened by a deep link, shown highlighted until the user moves on. */
    val highlighted: SettingsItemId? = null,
    /** The open choice dialog, or `null`. */
    val dialog: ChoiceDialog? = null,
)

/** One settings item with its current value. */
data class SettingsRow(
    val id: SettingsItemId,
    val value: RowValue,
)

/** What a row shows at its end. */
sealed interface RowValue {
    data class Switch(
        val checked: Boolean,
    ) : RowValue

    /** The chosen options, shown as a summary. */
    data class Chosen(
        val options: ImmutableList<SettingsOption>,
    ) : RowValue

    data object Opens : RowValue

    data class Action(
        val enabled: Boolean,
    ) : RowValue
}

/** A dialog choosing one ([multiple] = false) or several options of [id]. */
data class ChoiceDialog(
    val id: SettingsItemId,
    val options: ImmutableList<SettingsOption>,
    val selected: ImmutableSet<String>,
    val multiple: Boolean,
)

/** A page the settings home asks the app to open. */
data class SettingsEffect(
    val destination: SettingsDestination,
)

/** User actions of the settings home. */
@Immutable
data class SettingsHomeActions(
    val onTabSelected: (SettingsTab) -> Unit = {},
    val onQueryChanged: (String) -> Unit = {},
    val onRowClicked: (SettingsItemId) -> Unit = {},
    val onOptionClicked: (String) -> Unit = {},
    val onDialogDismissed: () -> Unit = {},
)

/** Builds rows and dialogs from stored settings. */
internal object SettingsStateMapper {
    fun rows(settings: GeneralSettings): ImmutableList<SettingsRow> =
        SettingsItemId.entries.map { SettingsRow(it, value(SettingsCatalog.control(it), settings)) }.toImmutableList()

    fun dialog(
        id: SettingsItemId,
        settings: GeneralSettings,
    ): ChoiceDialog? =
        when (val control = SettingsCatalog.control(id)) {
            is SettingsControl.Choice -> {
                val selected = setOf(control.read(settings)).toImmutableSet()
                ChoiceDialog(id, control.options.toImmutableList(), selected, false)
            }

            is SettingsControl.MultiChoice -> {
                ChoiceDialog(id, control.options.toImmutableList(), control.read(settings).toImmutableSet(), true)
            }

            is SettingsControl.TimeOfDay -> {
                val chosen = control.read(settings)
                val options = (timeOptions(control.stepMinutes) + chosen).distinct().sorted()
                val labelled = options.map { timeOption(it, settings) }.toImmutableList()
                ChoiceDialog(id, labelled, setOf(chosen.toString()).toImmutableSet(), false)
            }

            else -> {
                null
            }
        }

    private fun value(
        control: SettingsControl,
        settings: GeneralSettings,
    ): RowValue =
        when (control) {
            is SettingsControl.Toggle -> {
                RowValue.Switch(control.read(settings))
            }

            is SettingsControl.Choice -> {
                RowValue.Chosen(control.options.filter { it.key == control.read(settings) }.toImmutableList())
            }

            is SettingsControl.MultiChoice -> {
                val chosen = control.read(settings)
                RowValue.Chosen(control.options.filter { it.key in chosen }.toImmutableList())
            }

            is SettingsControl.Link -> {
                RowValue.Opens
            }

            SettingsControl.ClearRecentSearches -> {
                RowValue.Action(settings.hasRecentSearches)
            }

            is SettingsControl.TimeOfDay -> {
                RowValue.Chosen(persistentListOf(timeOption(control.read(settings), settings)))
            }
        }

    /** The minutes of the day every [step] minutes from midnight. */
    private fun timeOptions(step: Int): List<Int> = (0 until MINUTES_PER_DAY step step).toList()

    /** [minute] of the day as an option keyed by the minute and written `HH:mm` in the settings' digits. */
    internal fun timeOption(
        minute: Int,
        settings: GeneralSettings,
    ): SettingsOption {
        val text = "%02d:%02d".format(Locale.ROOT, minute / MINUTES_PER_HOUR, minute % MINUTES_PER_HOUR)
        return SettingsOption(minute.toString(), text = Numerals.localizeDigits(text, settings.numerals))
    }

    private const val MINUTES_PER_HOUR = 60
    private const val MINUTES_PER_DAY = 1_440
}

/** Settings search: an item matches when every word of the query starts a word of its title or keywords. */
internal object SettingsSearch {
    fun matches(
        query: String,
        title: String,
        keywords: String,
    ): Boolean {
        val words = keys(query)
        if (words.isEmpty()) return false
        val haystack = (listOf(title) + keywords.split('|')).flatMap(::keys)
        return words.all { word -> haystack.any { it.startsWith(word) } }
    }

    /** Search keys of the words of [text]; words are split at spaces and zero-width non-joiners. */
    private fun keys(text: String): List<String> =
        text.split(WORD_BREAK).map(PersianText::searchKey).filter(String::isNotEmpty)

    private val WORD_BREAK = Regex("[\\s‌]+")
}
