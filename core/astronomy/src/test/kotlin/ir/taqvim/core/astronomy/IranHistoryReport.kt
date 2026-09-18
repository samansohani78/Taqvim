/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import java.time.Month

/** Every criterion of the evening-decision refit on 1381–1405 (ADR-0041), the shipped one first. */
internal fun historyCriteria(
    model: LogisticCrescentModel,
    lenientModel: LogisticCrescentModel,
): List<EveningCriterion> =
    listOf(
        yallopCriterion(CrescentVisibilityClass.D, shipped = true),
        yallopCriterion(CrescentVisibilityClass.C),
        yallopCriterion(CrescentVisibilityClass.E),
        odehCriterion(OdehZone.B),
        odehCriterion(OdehZone.C),
        odehCriterion(OdehZone.D),
        model.criterion(),
        lenientModel.criterion("logistic, fitted on AH $LENIENT_ERA_START onward, five cities"),
    )

/** The logistic model fitted on the fit months only. */
internal val historyModel: LogisticCrescentModel by lazy {
    LogisticCrescentModel.fit(HistoryEvenings.all.filterNot { it.month.heldOut })
}

/**
 * The logistic model fitted on the fit months of AH [LENIENT_ERA_START] onward only: ADR-0041 step (b), since the
 * Calendar Center's practice before that year differs (see the eras below).
 */
internal val lenientHistoryModel: LogisticCrescentModel by lazy {
    LogisticCrescentModel.fit(HistoryEvenings.all.filter { !it.month.heldOut && it.month.year >= LENIENT_ERA_START })
}

internal const val LENIENT_ERA_START = 1428

/** Appends the evening-decision section of the calibration report. */
internal fun StringBuilder.appendEveningDecision(scores: List<EveningScore>) {
    val evenings = HistoryEvenings.all
    val held = evenings.count { it.month.heldOut }
    appendLine()
    appendLine("## Iran 1381–1405 SH: the evening of day 29 (ADR-0041)")
    appendLine()
    appendLine(
        "Each official month whose length the calendars print is one decision: given the official first day, does " +
            "the criterion give the month 29 days (crescent seen on the evening of day 29 at any of the five " +
            "cities) or 30? ${evenings.size} decisions: ${evenings.size - held} fit (calendars 1381–1400) and $held " +
            "held out (calendars 1401–1405; the digits of 1395, 1396, 1401 and 1402 are read from their glyphs). " +
            "The logistic models are fitted on the fit months only, the second on those of AH " +
            "$LENIENT_ERA_START onward.",
    )
    appendLine()
    appendLine("| Criterion | Fit | Held out |")
    appendLine("|---|---|---|")
    scores.forEach { score ->
        appendLine(
            "| ${score.criterion.name} | ${percent(score.fitShare)} (${score.fitAgreed}/${score.fitTotal}) | " +
                "${percent(score.heldShare)} (${score.heldAgreed}/${score.heldTotal}) |",
        )
    }
    appendLine()
    appendLine("Logistic weights (intercept, age, lag, ARCV, ARCL, W; standardised): ${weights(historyModel)}.")
    appendLine()
    appendLine("Fitted on AH $LENIENT_ERA_START onward: ${weights(lenientHistoryModel)}.")
    scores.firstOrNull { it.criterion.shipped }?.let { appendEras(it) }
    scores.forEach { appendMisses(it) }
}

/** Which way the shipped criterion misses, per era: the calendars' practice does not look constant. */
private fun StringBuilder.appendEras(shipped: EveningScore) {
    appendLine()
    ERAS.forEach { (name, years) ->
        val misses = shipped.misses.filter { it.evening.month.year in years }
        val stricter = misses.count { it.official > it.predicted }
        appendLine(
            "- AH $name: ${misses.size} shipped misses — $stricter where the calendar has 30 days and the criterion " +
                "29 (the calendar is stricter), ${misses.size - stricter} where it has 29 and the criterion 30 " +
                "(the calendar is more lenient).",
        )
    }
}

private val ERAS = listOf("1423–1427" to (1423 until LENIENT_ERA_START), "1428–1448" to (LENIENT_ERA_START..1448))

private fun StringBuilder.appendMisses(score: EveningScore) {
    appendLine()
    appendLine("### Missed by ${score.criterion.name}")
    appendLine()
    if (score.misses.isEmpty()) {
        appendLine("None.")
        return
    }
    appendLine("| Month | First day | Official | Criterion | Best Yallop q | Best Odeh V | Probable reason |")
    appendLine("|---|---|---|---|---|---|---|")
    score.misses.forEach { miss ->
        val month = miss.evening.month
        appendLine(
            "| ${month.label}${if (month.heldOut) " (held out)" else ""} | ${month.firstDay} | ${miss.official} d | " +
                "${miss.predicted} d | ${number(miss.evening.maxQ)} | ${number(miss.evening.maxV)} | ${reason(miss)} |",
        )
    }
}

/** A one-line, probable explanation of a miss, from the geometry and the calendar's own notes. */
internal fun reason(miss: EveningMiss): String {
    val evening = miss.evening
    val next = IranMonthHistory.months.firstOrNull { it.startJdn == evening.month.startJdn + miss.official }
    val q = evening.maxQ
    val base =
        when {
            next?.basis == "announced" -> {
                "length set by the official announcement of the next month (a sighting report)"
            }

            q == null -> {
                "the Moon sets before the Sun at all five cities, yet the calendar has 29 days"
            }

            abs(q - YALLOP_D_LIMIT) < MARGIN -> {
                "marginal: best q ${number(q)} is at Yallop's class D/E limit (−0.232)"
            }

            evening.maxV?.let { abs(it - ODEH_C_LIMIT) < MARGIN_V } == true -> {
                "marginal: best V ${number(evening.maxV)} is at Odeh's zone C/D limit (−0.96)"
            }

            miss.official == SHORT -> {
                "the calendar accepts a fainter crescent than the criterion (best class ${Yallop.classify(q)})"
            }

            else -> {
                "the calendar rejects a crescent the criterion counts (best class ${Yallop.classify(q)})"
            }
        }
    return if (evening.month.firstDay
            .plusDays(SHORT - 1L)
            .month in WINTER
    ) {
        "$base; winter evening"
    } else {
        base
    }
}

private fun abs(value: Double) = kotlin.math.abs(value)

private fun percent(share: Double) = "%.1f %%".format(share * PERCENT)

private fun number(value: Double?) = value?.let { "%.3f".format(it) } ?: "—"

private fun weights(model: LogisticCrescentModel) = model.weights.joinToString(", ") { "%.2f".format(it) }

private const val PERCENT = 100
private const val SHORT = 29
private const val YALLOP_D_LIMIT = -0.232
private const val ODEH_C_LIMIT = -0.96
private const val MARGIN = 0.03
private const val MARGIN_V = 0.3
private val WINTER = setOf(Month.NOVEMBER, Month.DECEMBER, Month.JANUARY, Month.FEBRUARY)
