/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import ir.taqvim.core.model.Coordinates
import java.time.LocalDate
import kotlin.time.Instant

private const val SECONDS_PER_DAY = 86_400L
private const val SECONDS_PER_DEGREE_OF_LONGITUDE = 240.0

/** How one means of looking fared in a published observation table. */
internal enum class Sighting {
    SEEN,
    NOT_SEEN,
    NOT_TRIED,
    ;

    internal companion object {
        /** Odeh's marks: V visible, I invisible, empty not tried. */
        fun ofMark(mark: String): Sighting =
            when (mark) {
                "V" -> SEEN
                "I" -> NOT_SEEN
                "" -> NOT_TRIED
                else -> error("unknown visibility mark '$mark'")
            }

        /** The best result of several means of looking at the same crescent. */
        fun best(sightings: List<Sighting>): Sighting = sightings.minBy { it.ordinal }
    }
}

/** The facts of one published crescent observation: where, on which local date, and whether it was a morning one. */
internal data class CrescentRecord(
    val number: Int,
    val place: Coordinates,
    val localDate: LocalDate,
    val morning: Boolean,
) {
    /** Local mean midnight starting [localDate]; the observation's sunset or sunrise is the first one after it. */
    val startOfLocalDay: Instant
        get() =
            Instant.fromEpochSeconds(
                localDate.toEpochDay() * SECONDS_PER_DAY - (place.longitude * SECONDS_PER_DEGREE_OF_LONGITUDE).toLong(),
            )

    /** Odeh's crescent at this record's best time. */
    fun odeh(): OdehObservation? =
        if (morning) Odeh.morning(place, startOfLocalDay) else Odeh.evening(place, startOfLocalDay)

    /** Yallop's crescent at this record's best time. */
    fun yallop(): CrescentObservation? =
        if (morning) Yallop.morning(place, startOfLocalDay) else Yallop.evening(place, startOfLocalDay)
}

/** Outcomes counted per predicted class: a confusion matrix of prediction against observation. */
internal class SightingTally<K : Comparable<K>>(
    outcomes: List<Pair<K, Sighting>>,
) {
    private val counts = outcomes.groupingBy { it }.eachCount()

    fun count(
        predicted: K,
        sighting: Sighting,
    ): Int = counts[predicted to sighting] ?: 0

    /** Share of the attempts (seen or not seen) in [predicted] classes that saw the crescent. */
    fun successShare(vararg predicted: K): Double {
        val seen = predicted.sumOf { count(it, Sighting.SEEN) }
        val attempts = seen + predicted.sumOf { count(it, Sighting.NOT_SEEN) }
        require(attempts > 0) { "no attempts in ${predicted.toList()}" }
        return seen.toDouble() / attempts
    }

    override fun toString(): String =
        counts.keys
            .map { it.first }
            .distinct()
            .sorted()
            .joinToString("; ") { key ->
                "$key: seen ${count(key, Sighting.SEEN)}, not seen ${count(key, Sighting.NOT_SEEN)}, " +
                    "not tried ${count(key, Sighting.NOT_TRIED)}"
            }
}
