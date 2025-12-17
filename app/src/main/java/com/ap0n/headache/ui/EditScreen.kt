package com.ap0n.headache.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ap0n.headache.domain.model.HeadacheEntry
import com.ap0n.headache.domain.model.QuestionType
import com.ap0n.headache.domain.model.WizardQuestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    viewModel: MainViewModel,
    headacheId: String,
    onNavigateBack: () -> Unit
) {
    val editState by viewModel.editState.collectAsState()
    val questions by viewModel.wizardQuestions.collectAsState()

    // Trigger load when screen opens
    LaunchedEffect(headacheId) {
        viewModel.loadHeadacheForEdit(headacheId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Entry") },
                actions = {
                    IconButton(onClick = {
                        viewModel.deleteHeadache(headacheId)
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        when (val state = editState) {
            is EditState.Loading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is EditState.Error -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { Text("Entry not found") }

            is EditState.Success -> {
                EditForm(
                    initialHeadache = state.headache,
                    initialFactors = state.factors,
                    questions = questions,
                    onSave = { timestamp, sev, notes, factors ->
                        viewModel.updateHeadache(
                            state.headache.id,
                            timestamp,
                            sev,
                            60,
                            notes,
                            factors
                        )
                        onNavigateBack()
                    },
                    modifier = Modifier.padding(padding)
                )
            }

            else -> {}
        }
    }
}

@Composable
fun EditForm(
    initialHeadache: HeadacheEntry,
    initialFactors: Map<String, Any>,
    questions: List<WizardQuestion>,
    onSave: (Long, Int, String, Map<String, Any>) -> Unit,
    modifier: Modifier = Modifier
) {
    var timestamp by remember { mutableLongStateOf(initialHeadache.timestamp) }
    var severity by remember { mutableFloatStateOf(initialHeadache.severity.toFloat()) }
    var notes by remember { mutableStateOf(initialHeadache.notes) }
    // Clone initial factors into a mutable map
    val answers = remember { mutableStateMapOf<String, Any>().apply { putAll(initialFactors) } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        DateTimeRow(timestamp = timestamp, onTimestampSelected = { timestamp = it })
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            "Core Details",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text("Severity: ${severity.toInt()}")
        Slider(value = severity, onValueChange = { severity = it }, valueRange = 0f..10f, steps = 9)

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            "Context Factors",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        questions.forEach { q ->
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(q.text, style = MaterialTheme.typography.bodyMedium)

                when (q.type) {
                    QuestionType.NUMERIC -> {
                        val currentVal = (answers[q.id] as? Number)?.toFloat() ?: (q.min ?: 0f)
                        Slider(
                            value = currentVal,
                            onValueChange = { answers[q.id] = it },
                            valueRange = (q.min ?: 0f)..(q.max ?: 10f)
                        )
                        Text(
                            "Value: ${"%.1f".format(currentVal)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    QuestionType.BOOLEAN -> {
                        val isChecked = answers[q.id] as? Boolean ?: false
                        Switch(
                            checked = isChecked,
                            onCheckedChange = { answers[q.id] = it }
                        )
                    }

                    QuestionType.SINGLE_CHOICE -> {
                        val selected = answers[q.id] as? String
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            q.options.forEach { opt ->
                                FilterChip(
                                    selected = selected == opt,
                                    onClick = { answers[q.id] = opt },
                                    label = { Text(opt) },
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }
                    }

                    QuestionType.TEXT -> {
                        val currentText = answers[q.id] as? String ?: ""
                        OutlinedTextField(
                            value = currentText,
                            onValueChange = { answers[q.id] = it },
                            label = { Text("Answer") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { onSave(timestamp, severity.toInt(), notes, answers) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Changes")
        }
    }
}
