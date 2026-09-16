/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.model

/**
 * Prayer-time calculation conventions named in docs/PLAN.md §6 A-10. Only the identifiers live here; the angles and
 * the calculation are implemented in `:core:praytimes` (T-401).
 */
public enum class PrayerMethod {
    /** Muslim World League. */
    MWL,

    /** Islamic Society of North America. */
    ISNA,

    /** Egyptian General Authority of Survey. */
    EGYPT,

    /** Umm al-Qura, Makkah. */
    MAKKAH,

    /** University of Islamic Sciences, Karachi. */
    KARACHI,

    /** Institute of Geophysics, University of Tehran. */
    TEHRAN,

    /** Shia Ithna-Ashari (Jafari). */
    JAFARI,

    /** Majlis Ugama Islam Singapura. */
    SINGAPORE,

    /** Union des Organisations Islamiques de France (12° angles). */
    FRANCE,

    /** Spiritual Administration of Muslims of Russia. */
    RUSSIA,

    /** Presidency of Religious Affairs of Türkiye (Diyanet İşleri Başkanlığı). */
    DIYANET,
}

/** Juristic convention for the Asr prayer: the object's shadow length relative to its height (A-10). */
public enum class AsrJuristic(
    /** Shadow factor added to the noon shadow: 1 (standard) or 2 (Hanafi). */
    public val shadowFactor: Int,
) {
    /** Shafi'i, Maliki, Hanbali and Jafari: shadow equals the object's height. */
    STANDARD(1),

    /** Hanafi: shadow equals twice the object's height. */
    HANAFI(2),
}
