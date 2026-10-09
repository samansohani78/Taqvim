/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import ir.taqvim.core.events.EventCategory

/**
 * One switch per kind of day, in the settings list rather than behind a dialog (T-302, T-1500).
 *
 * The categories used to be a single row called "Kinds of day shown" that opened a dialog of checkboxes, so a user
 * who wanted religious occasions gone had to guess that the phrase meant them and open it to find out. Each kind is
 * now its own row, labelled in the user's words.
 *
 * The category is the second of the two independent axes [ir.taqvim.core.events.EventVisibilityPolicy] applies — a
 * source says who publishes an observance, a category what kind of day it is — and nothing here filters anything
 * itself: these switches only write the set the policy reads.
 */
internal object EventCategoryControls {
    /** Every kind of day a user can switch, in the order they are shown. A user's own events are never filtered. */
    private val SWITCHES: Map<SettingsItemId, EventCategory> =
        mapOf(
            SettingsItemId.EVENT_CATEGORY_NATIONAL to EventCategory.NATIONAL,
            SettingsItemId.EVENT_CATEGORY_RELIGIOUS to EventCategory.RELIGIOUS,
            SettingsItemId.EVENT_CATEGORY_CULTURAL to EventCategory.CULTURAL,
            SettingsItemId.EVENT_CATEGORY_INTERNATIONAL to EventCategory.INTERNATIONAL,
            SettingsItemId.EVENT_CATEGORY_ASTRONOMICAL to EventCategory.ASTRONOMICAL,
        )

    /** The switch for [id], or `null` when [id] is not a kind of day. */
    fun controlFor(id: SettingsItemId): SettingsControl? = SWITCHES[id]?.let(::toggle)

    fun eventAndSearchControl(id: SettingsItemId): SettingsControl? =
        EventCategoryControls.controlFor(id) ?: when (id) {
            SettingsItemId.EVENT_SOURCES -> {
                SettingsCatalog.enumMultiChoice(
                    SettingsLabels.eventSources,
                    allowEmpty = true,
                    { it.enabledEventSources },
                ) { s, v ->
                    s.copy(enabledEventSources = v)
                }
            }

            SettingsItemId.HOLIDAYS_ONLY -> {
                SettingsCatalog.toggle({ it.holidaysOnly }) { s, v -> s.copy(holidaysOnly = v) }
            }

            SettingsItemId.SUBSCRIPTIONS -> {
                SettingsControl.Link(SettingsDestination.SUBSCRIPTIONS)
            }

            SettingsItemId.SUBSCRIPTIONS_NETWORK -> {
                SettingsCatalog.toggle(
                    { it.subscriptionsNetworkAllowed },
                ) { s, v -> s.copy(subscriptionsNetworkAllowed = v) }
            }

            SettingsItemId.REMEMBER_SEARCHES -> {
                SettingsCatalog.toggle({ it.rememberRecentSearches }) { s, v -> s.copy(rememberRecentSearches = v) }
            }

            SettingsItemId.CLEAR_SEARCHES -> {
                SettingsControl.ClearRecentSearches
            }

            else -> {
                null
            }
        }

    /**
     * Writing marks the categories as chosen ([GeneralSettings.eventCategoriesChosen]): until a user touches one,
     * every category is shown, and switching one off has to be remembered as a choice rather than read back as
     * "not chosen yet" and silently restored. Switching every one off is allowed, as it is for sources — "show no
     * occasions" is a real answer.
     */
    private fun toggle(category: EventCategory) =
        SettingsControl.Toggle(
            read = { category in it.enabledEventCategories },
            write = { settings, on ->
                val next =
                    if (on) settings.enabledEventCategories + category else settings.enabledEventCategories - category
                settings.copy(enabledEventCategories = next, eventCategoriesChosen = true)
            },
        )
}
