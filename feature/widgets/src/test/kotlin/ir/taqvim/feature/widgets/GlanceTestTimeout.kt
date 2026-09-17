/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Time a Glance unit test may take. Glance's 2 s default fails on busy CI runners (run 35215836532) although the
 * widgets compose in well under a second locally.
 */
internal val GLANCE_TEST_TIMEOUT: Duration = 30.seconds
