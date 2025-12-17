package com.ap0n.headache.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ap0n.headache.data.local.FactorEntity
import com.ap0n.headache.data.local.HeadacheDao
import com.ap0n.headache.data.local.toEntity
import com.ap0n.headache.domain.analytics.AnalyticsCalculator
import com.ap0n.headache.domain.model.CorrelationResult
import com.ap0n.headache.domain.model.HeadacheEntry
import com.ap0n.headache.domain.model.QuestionType
import com.ap0n.headache.domain.model.WizardQuestion
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// --- 1. EditState Defined as a Top-Level Class ---
sealed class EditState {
    object Loading : EditState()
    object Error : EditState()
    data class Success(
        val headache: HeadacheEntry,
        val factors: Map<String, Any>
    ) : EditState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dao: HeadacheDao,
    private val analyticsCalculator: AnalyticsCalculator,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // --- 2. Existing Home/Analytics State ---
    val headaches = dao.getAllHeadaches().map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // TODO: Delete this?
    private val _wizardQuestions = MutableStateFlow<List<WizardQuestion>>(emptyList())
    val wizardQuestions = dao.getAllQuestions()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        initializeQuestions()
    }

    private fun initializeQuestions() {
        viewModelScope.launch {
            // Only seed from JSON if the DB is empty
            if (dao.getQuestionCount() == 0) {
                try {
                    val json =
                        context.assets.open("wizard.json").bufferedReader().use { it.readText() }
                    val type = object : TypeToken<List<WizardQuestion>>() {}.type
                    val defaultQuestions: List<WizardQuestion> = gson.fromJson(json, type)

                    dao.insertQuestions(defaultQuestions.map { it.toEntity() })
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun saveQuestion(q: WizardQuestion) {
        viewModelScope.launch {
            dao.insertQuestion(q.toEntity())
        }
    }

    fun deleteQuestion(id: String) {
        viewModelScope.launch { dao.deleteQuestion(id) }
    }

    private val _analyticsReport = MutableStateFlow<List<CorrelationResult>>(emptyList())
    val analyticsReport = _analyticsReport.asStateFlow()

    // --- 3. New Edit State ---
    private val _editState = MutableStateFlow<EditState>(EditState.Loading)
    val editState = _editState.asStateFlow()

    init {
        loadWizard()
    }

    private fun loadWizard() {
        try {
            val json = context.assets.open("wizard.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<WizardQuestion>>() {}.type
            _wizardQuestions.value = gson.fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- 4. Logic for Creating New Entries ---
    fun saveHeadache(
        severity: Int,
        duration: Int,
        notes: String,
        responses: Map<String, Any>,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val headacheEntry = HeadacheEntry(
                id = UUID.randomUUID().toString(),
                timestamp = timestamp,
                severity = severity,
                durationMinutes = duration,
                notes = notes
            )
            dao.insertHeadache(headacheEntry.toEntity())

            val factors = responses.mapNotNull { (questionId, ans) ->
                val question = _wizardQuestions.value.find { it.id == questionId }
                if (question != null) {
                    FactorEntity(
                        id = UUID.randomUUID().toString(),
                        headacheId = headacheEntry.id,
                        key = questionId,
                        value = ans.toString(),
                        type = question.type,
                        timestamp = System.currentTimeMillis()
                    )
                } else null
            }
            dao.insertFactors(factors)
            refreshAnalytics()
        }
    }

    // --- 5. Logic for Editing Existing Entries ---
    fun loadHeadacheForEdit(id: String) {
        viewModelScope.launch {
            _editState.value = EditState.Loading
            val headache = dao.getHeadacheById(id)
            if (headache != null) {
                val factors = dao.getFactorsForHeadache(id)
                // Convert list of factors to a Map { "sleep_hours" -> 8.0 } for UI binding
                val factorMap = factors.associate { it.key to parseFactorValue(it) }

                // This line (112) should now work perfectly
                _editState.value = EditState.Success(headache.toDomain(), factorMap)
            } else {
                _editState.value = EditState.Error
            }
        }
    }

    fun updateHeadache(
        id: String,
        timestamp: Long,
        severity: Int,
        duration: Int,
        notes: String,
        responses: Map<String, Any>
    ) {
        viewModelScope.launch {
            // 1. Update Core Entry
            val entry = HeadacheEntry(id, timestamp, severity, duration, notes)
            dao.updateHeadache(entry.toEntity())

            // 2. Replace Factors (Delete all old, insert new)
            dao.deleteFactorsForHeadache(id)

            val newFactors = responses.mapNotNull { (key, value) ->
                val q = _wizardQuestions.value.find { it.id == key }
                if (q != null) {
                    FactorEntity(
                        id = UUID.randomUUID().toString(),
                        headacheId = id,
                        key = key,
                        value = value.toString(),
                        type = q.type,
                        timestamp = timestamp
                    )
                } else null
            }
            dao.insertFactors(newFactors)
            refreshAnalytics()
        }
    }

    fun deleteHeadache(id: String) {
        viewModelScope.launch {
            dao.deleteFactorsForHeadache(id)
            dao.deleteHeadache(id)
            refreshAnalytics()
        }
    }

    fun refreshAnalytics() {
        viewModelScope.launch {
            val allHeadaches =
                dao.getAllHeadaches().firstOrNull()?.map { it.toDomain() } ?: emptyList()
            val allFactors = dao.getAllFactors().map { it.toDomain() }
            _analyticsReport.value =
                analyticsCalculator.calculateCorrelations(allHeadaches, allFactors)
        }
    }

    private fun parseFactorValue(f: FactorEntity): Any {
        return when (f.type) {
            QuestionType.NUMERIC -> f.value.toFloatOrNull() ?: 0f
            QuestionType.BOOLEAN -> f.value.toBoolean()
            else -> f.value
        }
    }
}