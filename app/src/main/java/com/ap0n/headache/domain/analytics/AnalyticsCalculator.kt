package com.ap0n.headache.domain.analytics

import com.ap0n.headache.domain.model.*
import javax.inject.Inject
import kotlin.math.sqrt

/**
 * pure Kotlin logic for calculating correlations.
 */
class AnalyticsCalculator @Inject constructor() {

    fun calculateCorrelations(
        headaches: List<HeadacheEntry>,
        factors: List<FactorEntry>
    ): List<CorrelationResult> {
        // Group factors by Key
        val factorsByKey = factors.groupBy { it.key }
        val results = mutableListOf<CorrelationResult>()

        factorsByKey.forEach { (key, keyFactors) ->
            // Assume 1-to-1 mapping for simplicity: Factor linked to Headache by ID
            val pairedData = keyFactors.mapNotNull { factor ->
                val headache = headaches.find { it.id == factor.headacheId }
                if (headache != null) Pair(headache, factor) else null
            }
            if (pairedData.size > 2) {
                val type = keyFactors.first().type
                if (type == QuestionType.NUMERIC) {
                    val r = calculatePearson(pairedData)
                    results.add(
                        CorrelationResult(
                            key, type, r,
                            "Pearson Correlation: ${"%.2f".format(r)}",
                            pairedData.size
                        )
                    )
                } else if (type == QuestionType.BOOLEAN) {
                    // Simple difference in severity
                    val trueGroup = pairedData.filter { it.second.value.toBoolean() }
                    val falseGroup = pairedData.filter { !it.second.value.toBoolean() }

                    if (trueGroup.isNotEmpty() && falseGroup.isNotEmpty()) {
                        val avgTrue = trueGroup.map { it.first.severity }.average()
                        val avgFalse = falseGroup.map { it.first.severity }.average()
                        val diff = avgTrue - avgFalse
                        results.add(
                            CorrelationResult(
                                key, type, diff,
                                "Avg Severity diff: ${"%.2f".format(diff)} (True: ${
                                    "%.1f".format(
                                        avgTrue
                                    )
                                } vs False: ${"%.1f".format(avgFalse)})",
                                pairedData.size
                            )
                        )
                    }
                }
            }
        }
        return results.sortedByDescending { kotlin.math.abs(it.score) }
    }

    private fun calculatePearson(data: List<Pair<HeadacheEntry, FactorEntry>>): Double {
        val n = data.size.toDouble()
        val x = data.map { it.second.value.toDoubleOrNull() ?: 0.0 }
        val y = data.map { it.first.severity.toDouble() }

        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.zip(y).sumOf { it.first * it.second }
        val sumXSq = x.sumOf { it * it }
        val sumYSq = y.sumOf { it * it }

        val numerator = n * sumXY - sumX * sumY
        val denominator = sqrt((n * sumXSq - sumX * sumX) * (n * sumYSq - sumY * sumY))

        return if (denominator == 0.0) 0.0 else numerator / denominator
    }
}