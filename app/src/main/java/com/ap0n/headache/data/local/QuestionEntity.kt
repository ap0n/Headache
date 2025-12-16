package com.ap0n.headache.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ap0n.headache.domain.model.QuestionType
import com.ap0n.headache.domain.model.WizardQuestion

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: String,
    val text: String,
    val type: QuestionType,
    val options: String, // Stored as "Sunny,Cloudy,Rainy"
    val min: Float?,
    val max: Float?
) {
    fun toDomain(): WizardQuestion {
        return WizardQuestion(
            id = id,
            text = text,
            type = type,
            options = if (options.isNotEmpty()) options.split(",") else emptyList(),
            min = min,
            max = max
        )
    }
}

fun WizardQuestion.toEntity(): QuestionEntity {
    return QuestionEntity(
        id = id,
        text = text,
        type = type,
        options = options.joinToString(","),
        min = min,
        max = max
    )
}