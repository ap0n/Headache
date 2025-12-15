package com.ap0n.headache

import com.ap0n.headache.domain.analytics.AnalyticsCalculator
import com.ap0n.headache.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsTest {

    private val calculator = AnalyticsCalculator()

    @Test
    fun `test numeric correlation calculation`() {
        // Setup: High Sleep (8) -> Low Pain (2), Low Sleep (4) -> High Pain (8)
        // This should be a strong negative correlation (approx -1.0)
        val h1 = HeadacheEntry("h1", 100L, 2, 0, "")
        val h2 = HeadacheEntry("h2", 200L, 8, 0, "")

        val f1 = FactorEntry("f1", "h1", "sleep", "8.0", QuestionType.NUMERIC, 100L)
        val f2 = FactorEntry("f2", "h2", "sleep", "4.0", QuestionType.NUMERIC, 200L)
        // Add a middle ground to avoid division by zero edge cases in simple formulas
        val h3 = HeadacheEntry("h3", 300L, 5, 0, "")
        val f3 = FactorEntry("f3", "h3", "sleep", "6.0", QuestionType.NUMERIC, 300L)

        val result = calculator.calculateCorrelations(
            listOf(h1, h2, h3),
            listOf(f1, f2, f3)
        )

        val sleepResult = result.find { it.factorKey == "sleep" }
        assert(sleepResult != null)
        // Pearson should be negative (more sleep = less pain)
        assertEquals(-1.0, sleepResult!!.score, 0.1)
    }

    @Test
    fun `test boolean difference calculation`() {
        // True -> Severity 8, False -> Severity 2
        val h1 = HeadacheEntry("h1", 100L, 8, 0, "") // True
        val h2 = HeadacheEntry("h2", 200L, 2, 0, "") // False

        val f1 = FactorEntry("f1", "h1", "stress", "true", QuestionType.BOOLEAN, 100L)
        val f2 = FactorEntry("f2", "h2", "stress", "false", QuestionType.BOOLEAN, 200L)

        val result = calculator.calculateCorrelations(listOf(h1, h2), listOf(f1, f2))

        val stressResult = result.find { it.factorKey == "stress" }
        // Difference 8 - 2 = 6
        assertEquals(6.0, stressResult!!.score, 0.1)
    }
}