package com.ap0n.headache.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HeadacheDao {
    @Query("SELECT * FROM headaches ORDER BY timestamp DESC")
    fun getAllHeadaches(): Flow<List<HeadacheEntity>>

    @Query("SELECT * FROM factors WHERE headacheId = :headacheId")
    suspend fun getFactorsForHeadache(headacheId: String): List<FactorEntity>

    @Query("SELECT * FROM factors")
    suspend fun getAllFactors(): List<FactorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeadache(headache: HeadacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFactors(factors: List<FactorEntity>)

    @Query("DELETE FROM headaches WHERE id = :id")
    suspend fun deleteHeadache(id: String)

    @Query("DELETE FROM factors WHERE headacheId = :id")
    suspend fun deleteFactorsForHeadache(id: String)

    @Query("SELECT * FROM headaches WHERE id = :id")
    suspend fun getHeadacheById(id: String): HeadacheEntity?

    @Update
    suspend fun updateHeadache(headache: HeadacheEntity)

    @Query("SELECT * FROM questions")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getQuestionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun deleteQuestion(id: String)
}