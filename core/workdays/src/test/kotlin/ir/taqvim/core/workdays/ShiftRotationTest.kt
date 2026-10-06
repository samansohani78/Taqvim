/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.workdays

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Jdn
import org.junit.jupiter.api.Test

/** F-08: where a repeating shift pattern puts each day, and what an exception does to it. */
class ShiftRotationTest {
    private val day = ShiftType("Day")
    private val night = ShiftType("Night")
    private val off = ShiftType("Off")
    private val anchor = Jdn(2_461_000)

    private val rotation =
        ShiftRotation(
            id = 1,
            name = "Two on, two off",
            anchor = anchor,
            pattern = listOf(day, day, night, night, off, off),
        )

    @Test
    fun `the pattern repeats for ever in both directions`() {
        rotation.shiftOn(anchor) shouldBe day
        rotation.shiftOn(anchor + 2) shouldBe night
        rotation.shiftOn(anchor + 5) shouldBe off
        // The next cycle, and the one before the anchor: a rotation did not start the day the user wrote it down.
        rotation.shiftOn(anchor + 6) shouldBe day
        rotation.shiftOn(anchor - 1) shouldBe off
        rotation.shiftOn(anchor - 6) shouldBe day
        rotation.shiftOn(anchor - 7) shouldBe off
    }

    @Test
    fun `an exception wins over the pattern, for that one day only`() {
        val swapped = rotation.copy(exceptions = mapOf((anchor + 1).value to "Night"))

        swapped.shiftOn(anchor + 1) shouldBe night
        swapped.shiftOn(anchor) shouldBe day
        swapped.shiftOn(anchor + 7) shouldBe day
    }

    @Test
    fun `an exception naming a shift the pattern does not have still shows that shift`() {
        // Covering someone else's shift is exactly the case exceptions exist for, and the cover may be a kind of day
        // this rotation never has. It shows, without a colour, rather than being dropped.
        val covering = rotation.copy(exceptions = mapOf(anchor.value to "Training"))

        covering.shiftOn(anchor) shouldBe ShiftType("Training")
    }

    @Test
    fun `a rotation with no pattern says nothing about any day`() {
        val empty = rotation.copy(pattern = emptyList())

        empty.shiftOn(anchor).shouldBeNull()
        empty.shiftOn(anchor + 100).shouldBeNull()
    }

    @Test
    fun `the types are the distinct shifts in the order they first appear`() {
        rotation.types shouldBe listOf(day, night, off)
    }
}
