/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

/** A rule deciding, from the evening of day 29 at the calibration sites, whether the month has 29 days. */
internal class EveningCriterion(
    val name: String,
    val shipped: Boolean = false,
    val seen: (MonthEvening) -> Boolean,
)

/** One month the criterion decides differently from the official calendar. */
internal class EveningMiss(
    val evening: MonthEvening,
    val predicted: Int,
) {
    val official: Int get() = checkNotNull(evening.month.length)
}

/** Scores of one criterion on the fit and held-out months of the 1381–1405 history (ADR-0041). */
internal class EveningScore(
    val criterion: EveningCriterion,
    val fitAgreed: Int,
    val fitTotal: Int,
    val heldAgreed: Int,
    val heldTotal: Int,
    val misses: List<EveningMiss>,
) {
    val fitShare: Double get() = fitAgreed.toDouble() / fitTotal
    val heldShare: Double get() = heldAgreed.toDouble() / heldTotal
}

/** Scores [criterion] on every decided month of the history. */
internal fun scoreEvenings(
    criterion: EveningCriterion,
    evenings: List<MonthEvening> = HistoryEvenings.all,
): EveningScore {
    val misses =
        evenings.mapNotNull { evening ->
            val predicted = if (criterion.seen(evening)) SHORT_MONTH else SHORT_MONTH + 1
            EveningMiss(evening, predicted).takeIf { predicted != evening.month.length }
        }
    val (held, fit) = evenings.partition { it.month.heldOut }
    val missed = misses.map { it.evening }.toSet()
    return EveningScore(
        criterion,
        fit.count { it !in missed },
        fit.size,
        held.count { it !in missed },
        held.size,
        misses,
    )
}

private const val SHORT_MONTH = 29

/** Yallop's test at the five cities: seen when any site rates the crescent [visibleUpTo] or better. */
internal fun yallopCriterion(
    visibleUpTo: CrescentVisibilityClass,
    shipped: Boolean = false,
) = EveningCriterion(
    "Yallop ≤ $visibleUpTo, five cities${if (shipped) " (shipped, ADR-0027)" else ""}",
    shipped,
) { evening -> evening.sites.any { it.yallop.visibility <= visibleUpTo } }

/** Odeh's criterion at the five cities: seen when any site is in zone [visibleUpTo] or better. */
internal fun odehCriterion(visibleUpTo: OdehZone) =
    EveningCriterion("Odeh ≤ $visibleUpTo, five cities") { evening ->
        evening.sites.any { Odeh.classify(it.odehV) <= visibleUpTo }
    }

/**
 * A logistic model of the official decision on the best site's geometry: Moon age (h), lag (min), arc of vision ARCV,
 * arc of light ARCL (°) and crescent width W (′), standardised on the fit months. Fitted by Newton's method
 * (iteratively reweighted least squares) with a small ridge term so that separable data still converges.
 */
internal class LogisticCrescentModel private constructor(
    private val means: DoubleArray,
    private val scales: DoubleArray,
    val weights: DoubleArray,
) {
    /** Probability that the official calendar counts the crescent of [site] as seen. */
    fun probability(site: SiteEvening): Double = sigmoid(dot(weights, row(site)))

    /** Whether the crescent of [site] counts as seen: a probability of at least one half. */
    fun seen(site: SiteEvening): Boolean = probability(site) >= HALF

    /** The model as a criterion: seen when any of the five cities is. */
    fun criterion(): EveningCriterion =
        EveningCriterion("logistic (age, lag, ARCV, ARCL, W), five cities") { evening -> evening.sites.any(::seen) }

    private fun row(site: SiteEvening): DoubleArray = rowOf(features(site))

    companion object {
        private const val HALF = 0.5
        private const val RIDGE = 1e-3
        private const val MAX_ITERATIONS = 50
        private const val TOLERANCE = 1e-10

        /** Raw features of one site's evening. */
        fun features(site: SiteEvening): DoubleArray =
            doubleArrayOf(
                site.ageHours,
                site.yallop.lagMinutes,
                site.yallop.arcVisionDegrees,
                site.yallop.arcLightDegrees,
                site.yallop.widthArcMinutes,
            )

        /** Fits the model on [evenings] with a crescent at some site (the others carry no geometry to learn from). */
        fun fit(evenings: List<MonthEvening>): LogisticCrescentModel {
            val training = evenings.mapNotNull { e -> e.best?.let { features(it) to (e.month.length == SHORT_MONTH) } }
            val columns = training.first().first.size
            val means = DoubleArray(columns) { c -> training.map { it.first[c] }.average() }
            val scales =
                DoubleArray(columns) { c ->
                    sqrt(training.map { (it.first[c] - means[c]).let { d -> d * d } }.average()).takeIf { it > 0 }
                        ?: 1.0
                }
            val empty = LogisticCrescentModel(means, scales, DoubleArray(columns + 1))
            val rows = training.map { (features, _) -> empty.rowOf(features) }
            val labels = training.map { if (it.second) 1.0 else 0.0 }
            return LogisticCrescentModel(means, scales, newton(rows, labels))
        }

        private fun newton(
            rows: List<DoubleArray>,
            labels: List<Double>,
        ): DoubleArray {
            var weights = DoubleArray(rows.first().size)
            repeat(MAX_ITERATIONS) {
                val step = solve(hessian(rows, weights), gradient(rows, labels, weights))
                weights = DoubleArray(weights.size) { weights[it] + step[it] }
                if (step.maxOf { abs(it) } < TOLERANCE) return weights
            }
            return weights
        }

        private fun gradient(
            rows: List<DoubleArray>,
            labels: List<Double>,
            weights: DoubleArray,
        ): DoubleArray =
            DoubleArray(weights.size) { j ->
                rows.indices.sumOf { i -> (labels[i] - sigmoid(dot(weights, rows[i]))) * rows[i][j] } -
                    RIDGE * weights[j]
            }

        private fun hessian(
            rows: List<DoubleArray>,
            weights: DoubleArray,
        ): Array<DoubleArray> =
            Array(weights.size) { j ->
                DoubleArray(weights.size) { k ->
                    rows.sumOf { row -> sigmoid(dot(weights, row)).let { p -> p * (1 - p) } * row[j] * row[k] } +
                        if (j == k) RIDGE else 0.0
                }
            }

        /** Solves `a · x = b` by Gaussian elimination with partial pivoting (a is symmetric positive definite). */
        private fun solve(
            a: Array<DoubleArray>,
            b: DoubleArray,
        ): DoubleArray {
            val n = b.size
            val m = Array(n) { r -> a[r].copyOf() + b[r] }
            for (col in 0 until n) {
                val pivot = (col until n).maxBy { abs(m[it][col]) }
                m[col] = m[pivot].also { m[pivot] = m[col] }
                for (r in col + 1 until n) {
                    val factor = m[r][col] / m[col][col]
                    for (c in col..n) m[r][c] -= factor * m[col][c]
                }
            }
            val x = DoubleArray(n)
            for (r in n - 1 downTo 0) x[r] = (m[r][n] - (r + 1 until n).sumOf { m[r][it] * x[it] }) / m[r][r]
            return x
        }

        private fun sigmoid(z: Double): Double = 1 / (1 + exp(-z))

        private fun dot(
            a: DoubleArray,
            b: DoubleArray,
        ): Double = a.indices.sumOf { a[it] * b[it] }
    }

    private fun rowOf(features: DoubleArray): DoubleArray =
        DoubleArray(features.size + 1) { i -> if (i == 0) 1.0 else (features[i - 1] - means[i - 1]) / scales[i - 1] }
}
