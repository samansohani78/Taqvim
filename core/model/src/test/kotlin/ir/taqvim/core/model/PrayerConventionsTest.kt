/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PrayerConventionsTest {
    @Test
    fun `every method of plan A-10 has an identifier`() {
        PrayerMethod.entries.map { it.name } shouldBe
            listOf("MWL", "ISNA", "EGYPT", "MAKKAH", "KARACHI", "TEHRAN", "JAFARI", "SINGAPORE", "FRANCE", "RUSSIA")
    }

    @Test
    fun `asr shadow factors are one and two`() {
        AsrJuristic.entries.associateWith { it.shadowFactor } shouldBe
            mapOf(AsrJuristic.STANDARD to 1, AsrJuristic.HANAFI to 2)
    }
}
