/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

/**
 * A day or year outside what a calendar can express (BUG-1): the Hebrew and Nepali year tables, the lunar month
 * sequences and the computus formulas each cover a finite span, and a screen that reaches past it gets this instead of
 * a bare [IllegalArgumentException]. It stays an [IllegalArgumentException] so callers that already reject invalid
 * input keep working, and screens catch only this type, never an arbitrary failure (see `CalendarRangeGuard`).
 */
public class CalendarRangeException(
    message: String,
) : IllegalArgumentException(message)

/** Throws [CalendarRangeException] with [lazyMessage] unless [inRange]; the range twin of `require`. */
public inline fun requireInCalendarRange(
    inRange: Boolean,
    lazyMessage: () -> String,
) {
    if (!inRange) throw CalendarRangeException(lazyMessage())
}
