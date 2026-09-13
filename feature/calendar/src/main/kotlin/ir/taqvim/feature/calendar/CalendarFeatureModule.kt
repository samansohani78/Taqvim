/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin bindings of the calendar screen (ADR-0002). `:app` provides [CalendarSettingsSource], [CalendarDaySource],
 * [EventSearchSource] and `TodayProvider`.
 */
val calendarFeatureModule =
    module {
        factory<TodaySource> { TickingTodaySource(get()) }
        factory { SearchEventsUseCase(get()) }
        viewModelOf(::CalendarViewModel)
    }
