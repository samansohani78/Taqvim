/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.year

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin bindings of the year view (ADR-0002). `:app` provides [YearSettingsSource], [YearDaysSource] and
 * [YearTodaySource].
 */
val yearFeatureModule =
    module {
        viewModelOf(::YearViewModel)
    }
