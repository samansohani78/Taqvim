/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1200 per-widget configuration: defaults, normalization and appearance arithmetic. */
class WidgetConfigTest {
    @Test
    fun `new widgets show every part their kind offers`() {
        WidgetConfig.defaultFor(WidgetKind.MOON).contents shouldBe persistentSetOf()
        WidgetConfig.defaultFor(WidgetKind.DAY_SUMMARY_2X2).contents shouldBe WidgetContent.entries.toSet()
        WidgetConfig.defaultFor(WidgetKind.DATE_1X1).shows(WidgetContent.EVENTS).shouldBeFalse()
        WidgetConfig.defaultFor(WidgetKind.DATE_1X1).shows(WidgetContent.WEEKDAY).shouldBeTrue()
    }

    @Test
    fun `only countdown widgets keep a countdown, with a capped title`() {
        val countdown =
            WidgetCountdown(CalendarSystem.PERSIAN, 1405, 0, -3, startJdn = 1, title = "x".repeat(60))

        WidgetConfig(countdown = countdown).normalizedFor(WidgetKind.MOON).countdown.shouldBeNull()
        val kept = requireNotNull(WidgetConfig(countdown = countdown).normalizedFor(WidgetKind.COUNTDOWN).countdown)
        kept.title.length shouldBe WidgetCountdown.MAX_TITLE_LENGTH
        kept.month shouldBe 1
        kept.day shouldBe 1
        kept.normalized() shouldBe kept
        WidgetConfig.defaultFor(WidgetKind.COUNTDOWN).countdown.shouldBeNull()
    }

    @Test
    fun `values are brought into range for the kind`() {
        val kind = WidgetKind.DATE_1X1

        fun normalized(
            transparency: Int = 0,
            scale: Int = 100,
        ) = WidgetConfig(transparencyPercent = transparency, scalePercent = scale).normalizedFor(kind)

        normalized(transparency = -5).transparencyPercent shouldBe 0
        normalized(transparency = 95).transparencyPercent shouldBe WidgetConfig.MAX_TRANSPARENCY
        normalized(transparency = 44).transparencyPercent shouldBe 40
        normalized(transparency = 45).transparencyPercent shouldBe 50
        normalized(scale = 110).scalePercent shouldBe 100
        normalized(scale = 140).scalePercent shouldBe 150
        normalized(scale = 10).scalePercent shouldBe 75
        normalized().contents shouldBe kind.contents
    }

    @Test
    fun `normalization is idempotent and always in range`(): Unit =
        runBlocking {
            checkAll(
                PropertyTesting.iterations,
                Arb.enum<WidgetKind>(),
                Arb.int(-200..300),
                Arb.int(-100..400),
                contentSubsets(),
            ) { kind, transparency, scale, contents ->
                val config =
                    WidgetConfig(
                        transparencyPercent = transparency,
                        scalePercent = scale,
                        contents = contents.toImmutableSet(),
                    ).normalizedFor(kind)
                config.normalizedFor(kind) shouldBe config
                config.transparencyPercent shouldBeInRange 0..WidgetConfig.MAX_TRANSPARENCY
                (config.transparencyPercent % WidgetConfig.TRANSPARENCY_STEP) shouldBe 0
                config.scalePercent shouldBeIn WidgetConfig.SCALES
                kind.contents.containsAll(config.contents).shouldBeTrue()
            }
        }

    /** Every subset of the widget parts, each part picked by its own coin flip, so no size is ever unreachable. */
    private fun contentSubsets(): Arb<Set<WidgetContent>> {
        val parts = WidgetContent.entries
        return Arb.list(Arb.boolean(), parts.size..parts.size).map { picks ->
            parts.filterIndexed { index, _ -> picks[index] }.toSet()
        }
    }

    @Test
    fun `transparency reduces alpha and keeps the color`() {
        WidgetAppearance.withTransparency(0xFF112233.toInt(), 0) shouldBe 0xFF112233.toInt()
        WidgetAppearance.withTransparency(0xFF112233.toInt(), 100) shouldBe 0x00112233
        WidgetAppearance.withTransparency(0xFF112233.toInt(), 50) shouldBe 0x80112233.toInt()
        WidgetAppearance.withTransparency(0x80112233.toInt(), 50) shouldBe 0x40112233
        WidgetAppearance.withTransparency(0xFF112233.toInt(), 150) shouldBe 0x00112233
        WidgetAppearance.withTransparency(0xFF112233.toInt(), -10) shouldBe 0xFF112233.toInt()
        WidgetAppearance.scaled(12f, 150) shouldBe (18f plusOrMinus 0.0001f)
        WidgetAppearance.scaled(16f, 75) shouldBe (12f plusOrMinus 0.0001f)
    }
}
