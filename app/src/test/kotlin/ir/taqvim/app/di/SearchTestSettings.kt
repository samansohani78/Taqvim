/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.data.events.EventsSettings
import ir.taqvim.data.events.toEventsSettings
import ir.taqvim.data.preferences.UserPreferences
import kotlinx.datetime.TimeZone

/**
 * Settings with nothing switched off, for tests about something other than visibility.
 *
 * Search takes its settings rather than defaulting to them on purpose: a call site that forgets them is how the
 * dataset came to be searched with no visibility policy at all, so the requirement stays explicit everywhere.
 */
internal fun everythingShown(zone: TimeZone = TimeZone.of("Asia/Tehran")): EventsSettings =
    UserPreferences.defaultsFor("fa").toEventsSettings(homeTimeZone = zone)
