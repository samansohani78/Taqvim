/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin bindings of the calendar screen (ADR-0002). `:app` provides [CalendarSettingsSource], [CalendarDaySource],
 * [CalendarMonthSource], [EventSearchSource], [CalendarPlaceSource], `TodayProvider` and `kotlin.time.Clock`.
 */
val calendarFeatureModule =
    module {
        factory<TodaySource> { TickingTodaySource(get()) }
        factory<NowSource> { MinuteNowSource(get()) }
        factory { SearchEventsUseCase(get()) }
        viewModel { CalendarViewModel(get(), get(), get(), get(), get(), get(), get()) }
    }
