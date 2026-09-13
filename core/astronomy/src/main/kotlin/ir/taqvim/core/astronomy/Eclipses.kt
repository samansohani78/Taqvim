/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.github.cosinekitty.astronomy.EclipseEvent
import io.github.cosinekitty.astronomy.EclipseKind as LibraryEclipseKind
import io.github.cosinekitty.astronomy.GlobalSolarEclipseInfo
import io.github.cosinekitty.astronomy.LocalSolarEclipseInfo
import io.github.cosinekitty.astronomy.LunarEclipseInfo
import io.github.cosinekitty.astronomy.nextGlobalSolarEclipse as libraryNextGlobalSolarEclipse
import io.github.cosinekitty.astronomy.nextLunarEclipse as libraryNextLunarEclipse
import io.github.cosinekitty.astronomy.searchGlobalSolarEclipse as librarySearchGlobalSolarEclipse
import io.github.cosinekitty.astronomy.searchLocalSolarEclipse as librarySearchLocalSolarEclipse
import io.github.cosinekitty.astronomy.searchLunarEclipse as librarySearchLunarEclipse
import ir.taqvim.core.model.Coordinates
import kotlin.time.Instant

/** Eclipse types. */
public enum class EclipseKind {
    PENUMBRAL,
    PARTIAL,
    ANNULAR,
    TOTAL,
}

/** A solar eclipse seen anywhere on Earth: [peak] instant and, for central eclipses, where it is greatest. */
public data class GlobalSolarEclipse(
    public val kind: EclipseKind,
    public val peak: Instant,
    public val obscuration: Double?,
    public val peakLatitudeDegrees: Double?,
    public val peakLongitudeDegrees: Double?,
)

/** A lunar eclipse: [peak], Moon [obscuration] and the semi-duration of each phase in minutes (0 if absent). */
public data class LunarEclipse(
    public val kind: EclipseKind,
    public val peak: Instant,
    public val obscuration: Double,
    public val penumbralSemiDurationMinutes: Double,
    public val partialSemiDurationMinutes: Double,
    public val totalSemiDurationMinutes: Double,
)

/** A contact of a local solar eclipse and the Sun's altitude (degrees) at that moment. */
public data class EclipseContact(
    public val instant: Instant,
    public val sunAltitudeDegrees: Double,
)

/** A solar eclipse as seen from one place; total/annular contacts are `null` for partial eclipses. */
public data class LocalSolarEclipse(
    public val kind: EclipseKind,
    public val obscuration: Double,
    public val partialBegin: EclipseContact,
    public val totalBegin: EclipseContact?,
    public val peak: EclipseContact,
    public val totalEnd: EclipseContact?,
    public val partialEnd: EclipseContact,
)

/** Eclipse searches (A-13). */
public object Eclipses {
    /** Solar eclipses with their peak from [from] until [until]. */
    public fun solarEclipses(
        from: Instant,
        until: Instant,
    ): List<GlobalSolarEclipse> =
        generateSequence(librarySearchGlobalSolarEclipse(from.toAstronomyTime())) {
            libraryNextGlobalSolarEclipse(it.peak)
        }.takeWhile { it.peak.toInstant() < until }
            .map { it.toGlobal() }
            .toList()

    /** Lunar eclipses with their peak from [from] until [until]. */
    public fun lunarEclipses(
        from: Instant,
        until: Instant,
    ): List<LunarEclipse> =
        generateSequence(librarySearchLunarEclipse(from.toAstronomyTime())) { libraryNextLunarEclipse(it.peak) }
            .takeWhile { it.peak.toInstant() < until }
            .map { it.toLunar() }
            .toList()

    /** The first solar eclipse visible from [observer] after [after]. */
    public fun nextLocalSolarEclipse(
        after: Instant,
        observer: Coordinates,
    ): LocalSolarEclipse = librarySearchLocalSolarEclipse(after.toAstronomyTime(), observer.toObserver()).toLocal()

    private fun LibraryEclipseKind.toKind(): EclipseKind =
        when (this) {
            LibraryEclipseKind.Penumbral -> EclipseKind.PENUMBRAL
            LibraryEclipseKind.Partial -> EclipseKind.PARTIAL
            LibraryEclipseKind.Annular -> EclipseKind.ANNULAR
            LibraryEclipseKind.Total -> EclipseKind.TOTAL
        }

    private fun Double.finiteOrNull(): Double? = takeIf { it.isFinite() }

    private fun GlobalSolarEclipseInfo.toGlobal() =
        GlobalSolarEclipse(
            kind.toKind(),
            peak.toInstant(),
            obscuration.finiteOrNull(),
            latitude.finiteOrNull(),
            longitude.finiteOrNull(),
        )

    private fun LunarEclipseInfo.toLunar() =
        LunarEclipse(kind.toKind(), peak.toInstant(), obscuration, sdPenum, sdPartial, sdTotal)

    private fun EclipseEvent.toContact() = EclipseContact(time.toInstant(), altitude)

    private fun LocalSolarEclipseInfo.toLocal() =
        LocalSolarEclipse(
            kind = kind.toKind(),
            obscuration = obscuration,
            partialBegin = partialBegin.toContact(),
            totalBegin = totalBegin?.toContact(),
            peak = peak.toContact(),
            totalEnd = totalEnd?.toContact(),
            partialEnd = partialEnd.toContact(),
        )
}
