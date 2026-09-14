/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import ir.taqvim.core.model.Jdn
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin bindings of the calendar screen (ADR-0002). `:app` provides [CalendarSettingsSource], [CalendarDaySource],
 * [CalendarMonthSource], [EventSearchSource], [CalendarPlaceSource], [CalendarDisplayStore], `TodayProvider` and
 * `kotlin.time.Clock`. An optional [Jdn] parameter is the day selected when the screen opens.
 */
val calendarFeatureModule =
    module {
        factory<TodaySource> { TickingTodaySource(get()) }
        factory<NowSource> { MinuteNowSource(get()) }
        factory { SearchEventsUseCase(get()) }
        viewModel { parameters ->
            CalendarViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                initialDay = parameters.getOrNull<Jdn>(),
            )
        }
    }
