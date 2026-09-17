/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

/** The bundled official Iranian months (Ramadan 1446 – Ramadan 1448), used as a golden oracle in tests. */
internal object OfficialIranMonths {
    val overrides: IslamicMonthOverrides by lazy {
        IslamicMonthOverrides.parse(IslamicMonthOverrides.bundledIranOfficialText().orEmpty()).getOrThrow()
    }
}
