/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.workdays

import ir.taqvim.core.model.Jdn

/** One kind of shift in a rotation (F-08): the [label] a day shows and the ARGB [color] it draws in. */
public data class ShiftType(
    public val label: String,
    public val color: Int? = null,
) {
    init {
        require(label.isNotBlank()) { "a shift type needs a label" }
    }
}

/**
 * A repeating shift cycle (F-08): [pattern] is one shift per day and repeats for ever in both directions, with
 * [anchor] the day its first entry falls on.
 *
 * [exceptions] override single days — the swapped shift, the covered night — and win over the pattern. A day whose
 * shift is `null` is one the rotation says nothing about, which is what an empty pattern means everywhere.
 */
public data class ShiftRotation(
    public val id: Long,
    public val name: String,
    public val anchor: Jdn,
    public val pattern: List<ShiftType>,
    public val isActive: Boolean = true,
    public val exceptions: Map<Long, String> = emptyMap(),
) {
    /** The shift of [day]: its exception when it has one, else the pattern entry the cycle puts there. */
    public fun shiftOn(day: Jdn): ShiftType? {
        exceptions[day.value]?.let { label ->
            return pattern.firstOrNull { it.label == label } ?: ShiftType(label)
        }
        if (pattern.isEmpty()) return null
        val index = Math.floorMod(day - anchor, pattern.size.toLong()).toInt()
        return pattern[index]
    }

    /** Every distinct shift type of the pattern, in the order they first appear. */
    public val types: List<ShiftType> get() = pattern.distinctBy { it.label }
}
