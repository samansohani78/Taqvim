/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

/** Kinds of dataset problems (docs/PLAN.md §5.3, D-01). */
enum class IssueKind {
    /** The file is not a single well-formed JSON document. */
    MALFORMED_JSON,

    /** The document violates `dataset/events.v1.json`: fields, enums, a title without `fa` or `ne`, rule parameters, citations. */
    SCHEMA,

    /** Two records share an id (within a file or across files). */
    DUPLICATE_ID,

    /** A `RelativeToEvent` rule points at a missing event or at its own event. */
    UNKNOWN_EVENT_REFERENCE,

    /** A day (or day of year) that no month (or year) of the record's calendar has. */
    DAY_OUT_OF_RANGE,

    /** A validity without any year, or with `fromYear` after `toYear`. */
    INVALID_VALIDITY,

    /**
     * A per-year instance a rule could express (ADR-0036): a `Single` rule without `oneOffReason`, a `oneOffReason` on
     * another rule type, or one-off records on the same calendar day in different years.
     */
    ONE_OFF_RULE,

    /** A `titleReview` language tag that is not a key of the record's `title` (ADR-0042). */
    TITLE_REVIEW_UNKNOWN_LANGUAGE,
}

/** One problem found in [file] at [location] (a JSON path). */
data class DatasetIssue(
    val file: String,
    val location: String,
    val kind: IssueKind,
    val message: String,
) {
    override fun toString(): String = "$file: $location [$kind] $message"
}
