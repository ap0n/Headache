package com.ap0n.headache.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
            is EditState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is EditState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Entry not found") }
            is EditState.Success -> {
                EditForm(
                    initialHeadache = state.headache,
                    initialFactors = state.factors,
                    questions = questions,
                    onSave = { sev, notes, factors ->
                        viewModel.updateHeadache(state.headache.id, state.headache.timestamp, sev, 60, notes, factors)
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
    initialHeadache: com.ap0n.headache.domain.model.HeadacheEntry,
    initialFactors: Map<String, Any>,
    questions: List<WizardQuestion>,
    onSave: (Int, String, Map<String, Any>) -> Unit,
    modifier: Modifier = Modifier
) {
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
        Text("Core Details", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
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

        Text("Context Factors", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
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
                        Text("Value: ${"%.1f".format(currentVal)}", style = MaterialTheme.typography.bodySmall)
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
                    else -> {}
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { onSave(severity.toInt(), notes, answers) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Changes")
        }
    }
}