package com.ap0n.headache.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ap0n.headache.domain.model.HeadacheEntry
import com.ap0n.headache.domain.model.FactorEntry
import com.ap0n.headache.domain.model.QuestionType

@Entity(tableName = "headaches")
data class HeadacheEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val severity: Int,
    val durationMinutes: Int,
    val notes: String
) {
    fun toDomain() = HeadacheEntry(id, timestamp, severity, durationMinutes, notes)
}

@Entity(tableName = "factors")
data class FactorEntity(
    @PrimaryKey val id: String,
    val headacheId: String,
    val key: String,
    val value: String,
    val type: QuestionType,
    val timestamp: Long
) {
    fun toDomain() = FactorEntry(id, headacheId, key, value, type, timestamp)
}

fun HeadacheEntry.toEntity() = HeadacheEntity(id, timestamp, severity, durationMinutes, notes)
fun FactorEntry.toEntity() = FactorEntity(id, headacheId, key, value, type, timestamp)