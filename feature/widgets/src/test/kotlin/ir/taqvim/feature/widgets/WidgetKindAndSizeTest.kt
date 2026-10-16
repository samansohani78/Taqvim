/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.of
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1200 kinds and responsive size buckets. */
class WidgetKindAndSizeTest {
    /**
     * Every kind must support a bucket that actually matches the span it asks the launcher for.
     *
     * `forAvailable` picks the largest supported bucket that fits, so a kind whose target span has no bucket near
     * it is laid out for something much smaller — which is exactly what happened to T-1216: a 3x3 widget drawn
     * with type written for a 2x2 one, three lines of 13 sp and 10 sp in a 180 dp square. The rule is deliberately
     * about the span a widget asks for rather than about fonts, because the fonts were the symptom.
     */
    @Test
    fun `widgets are laid out for the span they ask for`() {
        WidgetKind.entries.forEach { kind ->
            val widthDp = WidgetSize.cellsToDp(kind.cells.columns).toFloat()
            val heightDp = WidgetSize.cellsToDp(kind.cells.rows).toFloat()
            val chosen = WidgetSize.forAvailable(widthDp, heightDp, kind.sizes)
            withClue("$kind spans ${widthDp}x$heightDp dp but lays out as $chosen") {
                (chosen.widthDp >= widthDp * MIN_SPAN_RATIO) shouldBe true
                (chosen.heightDp >= heightDp * MIN_SPAN_RATIO) shouldBe true
            }
        }
    }

    @Test
    fun `buckets follow the 70n minus 30 dp cell rule`() {
        WidgetSize.entries.associateWith { it.widthDp to it.heightDp } shouldBe
            mapOf(
                WidgetSize.SMALL to (40 to 40),
                WidgetSize.WIDE to (250 to 40),
                WidgetSize.MEDIUM to (110 to 110),
                WidgetSize.MEDIUM_LARGE to (180 to 180),
                WidgetSize.LARGE to (250 to 110),
                WidgetSize.EXTRA_LARGE to (250 to 250),
            )
    }

    @Test
    fun `the largest supported bucket that fits is chosen, else the smallest`() {
        val medium = listOf(WidgetSize.MEDIUM, WidgetSize.LARGE)
        WidgetSize.forAvailable(300f, 120f, medium) shouldBe WidgetSize.LARGE
        WidgetSize.forAvailable(200f, 200f, medium) shouldBe WidgetSize.MEDIUM
        WidgetSize.forAvailable(110f, 110f, medium) shouldBe WidgetSize.MEDIUM
        WidgetSize.forAvailable(30f, 30f, medium) shouldBe WidgetSize.MEDIUM
        WidgetSize.forAvailable(400f, 400f, listOf(WidgetSize.WIDE, WidgetSize.MEDIUM)) shouldBe WidgetSize.MEDIUM
        shouldThrow<IllegalArgumentException> { WidgetSize.forAvailable(100f, 100f, emptyList()) }
    }

    @Test
    fun `any available size gets a supported bucket that fits whenever one does`(): Unit =
        runBlocking {
            val supported = Arb.list(Arb.of(WidgetSize.entries), 1..5)
            checkAll(PropertyTesting.iterations, Arb.int(0..600), Arb.int(0..600), supported) { width, height, sizes ->
                val chosen = WidgetSize.forAvailable(width.toFloat(), height.toFloat(), sizes)
                sizes shouldContain chosen
                val fitting = sizes.filter { it.widthDp <= width && it.heightDp <= height }
                if (fitting.isNotEmpty()) {
                    (chosen in fitting).shouldBeTrue()
                    fitting.all { it.widthDp * it.heightDp <= chosen.widthDp * chosen.heightDp }.shouldBeTrue()
                }
            }
        }

    @Test
    fun `kinds have unique ids, offer sizes and all depend on appearance`() {
        WidgetKind.entries
            .map { it.id }
            .toSet()
            .size shouldBe WidgetKind.entries.size
        WidgetKind.entries.forEach { kind ->
            WidgetKind.byId(kind.id) shouldBe kind
            kind.sizes.isNotEmpty().shouldBeTrue()
            (WidgetDependency.APPEARANCE in kind.dependencies).shouldBeTrue()
        }
        WidgetKind.byId("retired") shouldBe null
        WidgetKind.DATE_1X1.cells shouldBe WidgetCells(1, 1)
        WidgetKind.DATE_CLOCK_4X1.cells shouldBe WidgetCells(4, 1)
        WidgetKind.DAY_SUMMARY_2X2.cells shouldBe WidgetCells(2, 2)
        WidgetKind.PRAYER_STRIP_4X2.cells shouldBe WidgetCells(4, 2)
        shouldThrow<IllegalArgumentException> { WidgetCells(0, 1) }
        shouldThrow<IllegalArgumentException> { WidgetCells(1, 9) }
    }

    private companion object {
        /** How close the chosen bucket must come to the span: three quarters, so a bucket may round down a little. */
        const val MIN_SPAN_RATIO = 0.75f
    }
}
