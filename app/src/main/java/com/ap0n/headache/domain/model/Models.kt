package com.ap0n.headache.domain.model

import java.util.UUID

// Enums
enum class QuestionType { NUMERIC, BOOLEAN, SINGLE_CHOICE, TEXT }

// Wizard Definitions
data class WizardQuestion(
    val id: String,
    val text: String,
    val type: QuestionType,
    val options: List<String> = emptyList(),
    val min: Float? = null,
    val max: Float? = null
)

// Data Entries
data class HeadacheEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long,
    val severity: Int, // 0-10
    val durationMinutes: Int,
    val notes: String
)

data class FactorEntry(
    val id: String = UUID.randomUUID().toString(),
    val headacheId: String, // Link to headache
    val key: String, // e.g., "sleep_hours"
    val value: String, // Stored as string, parsed based on context
    val type: QuestionType,
    val timestamp: Long
)

// Analytics
data class CorrelationResult(
    val factorKey: String,
    val factorType: QuestionType,
    val score: Double, // Pearson R or Odds Ratio
    val description: String,
    val count: Int
)